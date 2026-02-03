import express from 'express'
import { MongoClient, ObjectId } from 'mongodb'
import cors from 'cors'

const app = express()
const PORT = process.env.PORT ?? 3000
const MONGODB_URL = process.env.MONGODB_URL ?? 'mongodb://localhost:27017/inbox'
const MONGODB_COLLECTION = process.env.MONGODB_COLLECTION ?? 'messages'
const CONTEXT = 'http://www.w3.org/ns/ldp'
const ID_ROOT = process.env.ID_ROOT ?? 'http://inbox.rerum.io'
const RATE_LIMIT_WINDOW_MS = 60 * 60 * 1000 // 1 hour in milliseconds
const RATE_LIMIT_MAX_REQUESTS = 10 // Maximum requests per hour per IP

let db = null
let messagesCollection = null

// Initialize MongoDB connection
async function connectToDatabase() {
    try {
        const client = new MongoClient(MONGODB_URL)
        await client.connect()
        db = client.db()
        messagesCollection = db.collection(MONGODB_COLLECTION)
        console.log('Connected to MongoDB')
    } catch (error) {
        console.error('Failed to connect to MongoDB:', error.message)
        process.exit(1)
    }
}

// Middleware
app.use(cors())
app.use(express.json())

// Helper function to generate object with @id and strip __inbox metadata
function addIdToObject(obj, id) {
    const { _id, __inbox, ...rest } = obj
    return {
        '@id': id,
        ...rest
    }
}

// Helper function to get client IP address
function getClientIp(req) {
    return req.headers['x-forwarded-for']?.split(',')[0].trim() ||
           req.headers['x-real-ip'] ||
           req.socket.remoteAddress ||
           req.connection.remoteAddress ||
           'unknown'
}

// Middleware to check rate limit
async function checkRateLimit(req, res, next) {
    try {
        if (!messagesCollection) {
            return res.status(503).json({ error: 'Database connection not established' })
        }

        const clientIp = getClientIp(req)
        const now = new Date()
        const windowStart = new Date(now.getTime() - RATE_LIMIT_WINDOW_MS)

        // Count requests from this IP in the last hour
        const requestCount = await messagesCollection.countDocuments({
            '__inbox.ip': clientIp,
            '__inbox.timestamp': { $gte: windowStart }
        })

        if (requestCount >= RATE_LIMIT_MAX_REQUESTS) {
            return res.status(429).json({
                error: 'Rate limit exceeded. Please try again later.',
                retryAfter: Math.ceil(RATE_LIMIT_WINDOW_MS / 1000) // seconds
            })
        }

        next()
    } catch (error) {
        console.error('Error checking rate limit:', error.message)
        // Allow request to proceed if rate limit check fails
        next()
    }
}

// Helper function to build MongoDB query
function buildMongoQuery(target, type, motivation) {
    const query = {}
    if (type) query.type = type
    if (target) query.target = target
    if (motivation) query.motivation = motivation
    return query
}

// GET /messages - List all messages with optional filtering
app.get('/messages', async (req, res) => {
    try {
        if (!messagesCollection) {
            return res.status(503).json({ error: 'Database connection not established' })
        }

        const { target = '', type = '', motivation = '' } = req.query

        // Build MongoDB query
        const query = buildMongoQuery(target, type, motivation)

        // Fetch from MongoDB
        const messages = await messagesCollection.find(query).toArray()

        // Convert to array with @id added
        const formattedMessages = messages.map(msg =>
            addIdToObject(msg, `${ID_ROOT}/id/${msg._id.toString()}`)
        )

        // Format response as LDP Container
        const container = {
            '@context': CONTEXT,
            '@type': 'ldp:Container',
            '@id': `${ID_ROOT}/messages?target=${target}`,
            'contains': formattedMessages
        }

        res.json(container)
    } catch (error) {
        console.error('Error fetching messages:', error.message)
        res.status(500).json({ error: 'Failed to fetch messages' })
    }
})

// POST /messages - Create a new message
app.post('/messages', checkRateLimit, async (req, res) => {
    try {
        if (!messagesCollection) {
            return res.status(503).json({ error: 'Database connection not established' })
        }

        const announcement = req.body

        // Validation: Check for existing @id
        if (announcement['@id']) {
            return res.status(400).json({
                error: "Property '@id' indicates this is not a new announcement."
            })
        }

        // Validation: Check for required motivation
        if (!announcement.motivation) {
            return res.status(400).json({
                error: "Announcements without 'motivation' are not allowed on this server."
            })
        }

        // Add @context if missing
        announcement['@context'] ??= CONTEXT

        // Add timestamp
        announcement.published = new Date().toISOString()

        // Add internal metadata in __inbox field
        announcement.__inbox = {
            ip: getClientIp(req),
            referrer: req.headers.referer || req.headers.referrer || 'direct',
            userAgent: req.headers['user-agent'] || 'unknown',
            timestamp: new Date()
        }

        // Insert into MongoDB
        const result = await messagesCollection.insertOne(announcement)

        // Return the created announcement with @id (stripping __inbox)
        const returnObj = addIdToObject(
            { ...announcement, _id: result.insertedId },
            `${ID_ROOT}/id/${result.insertedId.toString()}`
        )

        console.log(`Created: ${result.insertedId.toString()}`)
        res.status(201).json(returnObj)
    } catch (error) {
        console.error('Error creating message:', error.message)
        res.status(500).json({ error: 'Failed to create message' })
    }
})

// GET /id/:noteId - Get a specific message by ID
app.get('/id/:noteId', async (req, res) => {
    try {
        if (!messagesCollection) {
            return res.status(503).json({ error: 'Database connection not established' })
        }

        const { noteId } = req.params

        // Validate MongoDB ObjectId format
        if (!ObjectId.isValid(noteId)) {
            return res.status(404).json({ error: 'No message found' })
        }

        // Fetch from MongoDB
        const message = await messagesCollection.findOne({ _id: new ObjectId(noteId) })

        if (!message) {
            return res.status(404).json({ error: 'No message found' })
        }

        // Add @id to the message
        const result = addIdToObject(message, `${ID_ROOT}/id/${noteId}`)

        res.json(result)
    } catch (error) {
        console.error(`Error fetching message ${req.params.noteId}:`, error.message)
        res.status(404).json({ error: 'No message found' })
    }
})

// PUT /messages - Not implemented
app.put('/messages', (req, res) => {
    res.status(405).json({ error: 'PUT is not implemented for this inbox.' })
})

// DELETE /messages - Not implemented
app.delete('/messages', (req, res) => {
    res.status(405).json({ error: 'DELETE is not implemented for this inbox.' })
})

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() })
})

// Start server
connectToDatabase().then(() => {
    app.listen(PORT, () => {
        console.log(`Rerum Inbox server running on port ${PORT}`)
        console.log(`ID_ROOT: ${ID_ROOT}`)
        console.log(`MongoDB: ${MONGODB_URL}`)
    })
})

export default app

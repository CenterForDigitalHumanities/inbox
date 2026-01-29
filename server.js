const express = require('express');
const axios = require('axios');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;
const FIREBASE_URL = 'https://rerum-inbox.firebaseio.com/messages';
const CONTEXT = 'http://www.w3.org/ns/ldp';
const ID_ROOT = process.env.ID_ROOT || 'http://inbox.rerum.io';

// Middleware
app.use(cors());
app.use(express.json());

// Helper function to generate object with @id
function addIdToObject(obj, id) {
    return {
        '@id': id,
        ...obj
    };
}

// Helper function to build Firebase query
function buildFirebaseQuery(target, type, motivation) {
    const params = new URLSearchParams();
    if (type) {
        params.append('orderBy', '"type"');
        params.append('equalTo', `"${type}"`);
    } else if (target) {
        params.append('orderBy', '"target"');
        params.append('equalTo', `"${target}"`);
    }
    return params.toString() ? `?${params.toString()}` : '';
}

// GET /messages - List all messages with optional filtering
app.get('/messages', async (req, res) => {
    try {
        const { target = '', type = '', motivation = '' } = req.query;
        
        // Build Firebase query
        const query = buildFirebaseQuery(target, type, motivation);
        const url = `${FIREBASE_URL}.json${query}`;
        
        // Fetch from Firebase
        const response = await axios.get(url);
        const data = response.data || {};
        
        // Convert object to array with @id added
        const messages = [];
        for (const [key, value] of Object.entries(data)) {
            const message = addIdToObject(value, `${ID_ROOT}/id/${key}`);
            
            // Filter by motivation if specified (client-side filtering)
            if (motivation && !message.motivation?.includes(motivation)) {
                continue;
            }
            
            messages.push(message);
        }
        
        // Format response as LDP Container
        const container = {
            '@context': CONTEXT,
            '@type': 'ldp:Container',
            '@id': `${ID_ROOT}/messages?target=${target}`,
            'contains': messages
        };
        
        res.json(container);
    } catch (error) {
        console.error('Error fetching messages:', error.message);
        res.status(500).json({ error: 'Failed to fetch messages' });
    }
});

// POST /messages - Create a new message
app.post('/messages', async (req, res) => {
    try {
        const announcement = req.body;
        
        // Validation: Check for existing @id
        if (announcement['@id']) {
            return res.status(400).json({
                error: "Property '@id' indicates this is not a new announcement."
            });
        }
        
        // Validation: Check for required motivation
        if (!announcement.motivation) {
            return res.status(400).json({
                error: "Announcements without 'motivation' are not allowed on this server."
            });
        }
        
        // Add @context if missing
        if (!announcement['@context']) {
            announcement['@context'] = CONTEXT;
        }
        
        // Add timestamp (using toString() for Java compatibility)
        announcement.published = new Date().toString();
        
        // Post to Firebase
        const response = await axios.post(`${FIREBASE_URL}.json`, announcement);
        
        // Firebase returns { "name": "generated-key" }
        if (!response.data || !response.data.name) {
            throw new Error('Invalid Firebase response');
        }
        const firebaseKey = response.data.name;
        
        // Return the created announcement with @id
        const result = addIdToObject(announcement, `${ID_ROOT}/id/${firebaseKey}`);
        
        console.log(`Created: ${firebaseKey}`);
        res.status(201).json(result);
    } catch (error) {
        console.error('Error creating message:', error.message);
        res.status(500).json({ error: 'Failed to create message' });
    }
});

// GET /id/:noteId - Get a specific message by ID
app.get('/id/:noteId', async (req, res) => {
    try {
        const { noteId } = req.params;
        const url = `${FIREBASE_URL}/${noteId}.json`;
        
        // Fetch from Firebase
        const response = await axios.get(url);
        
        if (!response.data || response.data === null) {
            return res.status(404).json({ error: 'No message found' });
        }
        
        // Add @id to the message
        const message = addIdToObject(response.data, `${ID_ROOT}/id/${noteId}`);
        
        res.json(message);
    } catch (error) {
        console.error(`Error fetching message ${req.params.noteId}:`, error.message);
        res.status(404).json({ error: 'No message found' });
    }
});

// PUT /messages - Not implemented
app.put('/messages', (req, res) => {
    res.status(405).json({ error: 'PUT is not implemented for this inbox.' });
});

// DELETE /messages - Not implemented
app.delete('/messages', (req, res) => {
    res.status(405).json({ error: 'DELETE is not implemented for this inbox.' });
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Start server
app.listen(PORT, () => {
    console.log(`Rerum Inbox server running on port ${PORT}`);
    console.log(`ID_ROOT: ${ID_ROOT}`);
});

module.exports = app;

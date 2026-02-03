# Inbox Repository

The Inbox is a Node.js/Express API for publishing and reading annotations and messages about digital resources on the Internet. It supports IIIF manifests and uses MongoDB for persistence.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Code Generation Preferences

Follow these guidelines when writing or modifying code:

- Do not use unnecessary semicolons - they are not needed in JavaScript
- Prefer ES6+ syntax - convert CommonJS to ES6+ (use `import`/`export` instead of `require`/`module.exports`)
- Use optional chaining (`?.`) and nullish coalescing (`??`) where possible
- Prefer guard clauses instead of nested if-else statements
- Avoid the use of whitelist and blacklist terminology
- Do not put script tags at the end of body tags if `defer` or `module` is sufficient

## Working Effectively

### Bootstrap, Build, and Test the Repository

- Install dependencies: `npm install` -- takes 10-15 seconds
- Run the application: `npm start` -- starts on port 3000
- Run in development mode: `npm run dev` -- starts with auto-reload on port 3000
- Test basic functionality: `curl http://localhost:3000/` should return the API status

### Environment Requirements

- Node.js >= 18 (v20+ recommended)
- MongoDB >= 4.0 (local or remote instance)
- `.env` file with configuration (see Environment Configuration section)

### Environment Configuration

The Inbox uses a simple environment-based configuration approach:

- `.env` - Local configuration (gitignored)
  - Contains sensitive data like Firebase credentials
  - Copy `.env.example` to `.env` and fill in values

Environment variables:

- **PORT** (default: 3000) - Server port
- **ID_ROOT** (default: http://inbox.rerum.io) - Base URL for generated annotation IDs
- **MONGODB_URL** (default: mongodb://localhost:27017/inbox) - MongoDB connection string
- **MONGODB_COLLECTION** (default: messages) - MongoDB collection name for messages
- **NODE_ENV** (default: development) - Runtime environment

## Validation

### Always Validate Core Functionality After Changes

- Start the application: `npm start` or `npm run dev`
- Test the root endpoint: `curl http://localhost:3000/` -- should return a simple status response
- Test the messages endpoint: `curl http://localhost:3000/messages` -- should return messages from Firebase
- Check application logs for errors

### Expected Behavior

- Application listens on configured PORT (default 3000)
- All endpoints are public (no authentication)
- Firebase operations fail gracefully if credentials are invalid
- CORS is enabled for cross-origin requests

## Common Tasks

### Repository Structure

```
/inbox/
├── server.js              # Express application entry point
├── package.json           # Dependencies and scripts
├── .env                   # Local configuration (gitignored)
├── .env.example           # Configuration template
├── .github/
│   ├── workflows/
│   │   └── nodejs-cicd.yml # CI/CD pipeline
│   └── copilot-instructions.md
├── readme.md              # Project documentation
└── DEPLOYMENT.md          # Deployment guide
```

### Key API Endpoints

- `GET /` -- Service status/health check
- `GET /messages` -- Get all messages with optional filtering
  - Query parameters: `target`, `type`, `motivation`
- `POST /messages` -- Create a new message
- `GET /messages/:id` -- Get a specific message
- `PUT /messages/:id` -- Update a message
- `DELETE /messages/:id` -- Delete a message

### Database Configuration

The application uses MongoDB for persistence:

- Connection URL: `MONGODB_URL` environment variable
- Collection name: `MONGODB_COLLECTION` environment variable (default: messages)
- Database persists annotations and messages
- Supports flexible schema with document-based storage

### Development Workflow

1. Always start with: `npm install`
2. Make code changes
3. Test with: `curl` commands or REST client
4. Start dev server: `npm run dev`
5. Review logs for errors and debug

### Running the Application

```bash
# Production
npm start

# Development (with auto-reload)
npm run dev

# Check if running
curl http://localhost:3000/messages
```

### Debugging and Troubleshooting

- Application logs appear in console when running `npm start` or `npm run dev`
- MongoDB connection errors indicate missing database services or invalid connection string
- Port conflicts indicate another service is using port 3000
- CORS errors indicate cross-origin requests are being blocked

### CI/CD Integration

GitHub Actions workflows in `.github/workflows/`:

- `nodejs-cicd.yml` - Test on PR, deploy on push to main
  - Triggers: Push to `main`, PR to `development`
  - Runs on `ubuntu-latest`
  - Requires Node.js 24

### Performance Notes

- Application startup: 1-2 seconds
- npm install: ~10-15 seconds (timeout: 30+ seconds)
- Firebase API calls: 500-2000ms depending on network

### Critical Environment Variables

Required for basic functionality:

- `PORT` (default: 3000)
- `ID_ROOT` (default: http://inbox.rerum.io)

Required for database functionality:

- `MONGODB_URL` (e.g., mongodb://localhost:27017/inbox)
- `MONGODB_COLLECTION` (default: messages)

Optional:

- `NODE_ENV` (default: development)

### Manual Testing Scenarios

After making changes, always validate:

1. **Basic Service**: Start server with `npm start`, test with `curl http://localhost:3000/` - should respond
2. **Messages Endpoint**: `curl http://localhost:3000/messages` - should return JSON array or object
3. **CORS Support**: Headers should include `Access-Control-Allow-Origin`
4. **Error Handling**: Invalid endpoints should return appropriate HTTP status codes

### Complete Validation Workflow Example

```bash
# Basic setup
npm install

# Test application serving
npm start &
sleep 2
curl http://localhost:3000/messages  # Should return JSON
curl http://localhost:3000/messages?type=Annotation  # Filtered query
kill %1  # Stop the background server
```

### Common File Locations

- Main application entry: `server.js`
- Dependencies: `package.json`
- Configuration: `.env` (local), `.env.example` (template)
- API documentation: `readme.md`
- Deployment info: `DEPLOYMENT.md`

### Dependencies and Versions

- Express.js - REST API framework
- MongoDB Node Driver - Database client for MongoDB
- CORS - Cross-Origin Resource Sharing
- Node.js - Runtime environment (v18+)

## Branching Strategy

- `main` - Main/production branch
- `development` - Development branch for feature work
- Feature branches - Branch from `development` for new work

All pull requests should target `development` unless it's a hotfix for production.

### Pull Request Process

1. Create feature branch from `development`
2. Make changes and commit
3. Push and create PR to `development`
4. GitHub Actions will test automatically
5. After review and approval, merge to `development`
6. Deploy to production via `main` branch

NEVER CANCEL long-running commands. Build and test are designed to complete within documented timeouts. Always wait for completion to ensure accurate validation of changes.

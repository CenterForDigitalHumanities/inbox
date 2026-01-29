# Rerum Inbox

The Rerum Inbox is a place for anyone to post and read announcements about any digital resource published on the Internet. A strong supporter of the IIIF standard, it is the sc:Manifest objects for which this has been created. The millions of images made available by libraries, museums, universities, and others throughout the world are now enriched by the linked knowledge of a planet full of scholars and researchers. Only the most relevant and intentional notes are intended for this announcement inbox, so the information is more immediately useful than aggregations that simply crawl the graph of Linked Data looking for relationships without discrimination.

## Technology Stack

This application is built with:
- **Node.js** - JavaScript runtime
- **Express** - Web application framework
- **Axios** - HTTP client for Firebase communication
- **Firebase Realtime Database** - Data storage backend

### A Brief History
The Rerum Inbox is the fruit of a collaboration that began in 2016 to share related IIIF data between institutions. With a generous grant from the IIIF Consortium, Jeffrey Witt (Loyola University Maryland) and Rafael Schwemmer (text & bytes) presented a proof of concept at the 2016 IIIF Conferences in New York using a web standard called WebMentions. At the Vatican 2017 IIIF Conference Jeffrey Witt and Chip Goines produced a second round of demonstrations, this time using an emerging standard called Linked Data Notifications. Following the presentation, new implementations were initiated with Régis Robineau of Biblissima.

The growing list of implementations and uses cases exposed the need for a well-described specification and a general use inbox which did not require the hosting institutions to maintain their own. In August of 2017, Patrick Cuba (Lead Developer, Walter J. Ong, S.J. Center for Digital Humanities), Jeffrey Witt, and Régis Robineau sat down to draft this specification. While Jeffrey Witt and Régis Robineau began the development of a Mirador Plugin that uses the established specification, Patrick Cuba created an exemplar inbox for a general use to which the plugin was linked. RERUM offered a home for this that extended the mission of creating and sharing open and public knowledge around important research. In this way, the circle was closed, allowing data to be shared between systems with requiring any changes on the part of manifest publishers.

### Primary Purpose
As a service of OngCDH, the Rerum Inbox is committed to providing a free and public location for important announcements about scholarly resources. This offers a service to individuals and institutions without the financial or technical means to host their own inbox and creates a path for interaction with the vast holdings on the Internet that are otherwise inaccessible. By generating new inboxes dynamically, Rerum Inbox immediately and universally opens the resources used by the scholarly community to the contributions of the scholars it comprises.

## API Endpoints

### GET /messages
List all messages with optional filtering.

**Query Parameters:**
- `target` - Filter by target URL
- `type` - Filter by message type
- `motivation` - Filter by motivation

**Response:**
```json
{
  "@context": "http://www.w3.org/ns/ldp",
  "@type": "ldp:Container",
  "@id": "http://inbox.rerum.io/messages?target=",
  "contains": [...]
}
```

### POST /messages
Create a new announcement.

**Request Body:**
```json
{
  "@type": "Announce",
  "motivation": "iiif:supplement:range",
  "object": "http://example.org/object",
  "target": "http://example.org/target",
  "actor": {
    "@id": "https://example.org/#identity",
    "label": "Example"
  }
}
```

**Response:**
```json
{
  "@id": "http://inbox.rerum.io/id/{generated-id}",
  "@context": "http://www.w3.org/ns/ldp",
  "@type": "Announce",
  "motivation": "iiif:supplement:range",
  "published": "2024-01-29T23:00:00.000Z",
  ...
}
```

### GET /id/:noteId
Retrieve a specific message by ID.

**Response:**
```json
{
  "@id": "http://inbox.rerum.io/id/{noteId}",
  ...
}
```

### GET /health
Health check endpoint.

**Response:**
```json
{
  "status": "ok",
  "timestamp": "2024-01-29T23:00:00.000Z"
}
```

## Installation

### Prerequisites
- Node.js 18.x or later
- npm

### Local Development

1. Clone the repository:
   ```bash
   git clone https://github.com/CenterForDigitalHumanities/inbox.git
   cd inbox
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```

4. The server will start on port 3000 (or PORT environment variable)

### Environment Variables

- `PORT` - Server port (default: 3000)
- `ID_ROOT` - Base URL for generated IDs (default: http://inbox.rerum.io)

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed deployment instructions, including:
- Required repository secrets
- Server requirements
- Apache/Nginx configuration
- PM2 process management
- Troubleshooting

## Testing

The application includes input validation:
- Rejects announcements with existing `@id`
- Requires `motivation` property
- Automatically adds `@context` if missing
- Adds `published` timestamp

## License

See [LICENSE](LICENSE) file for details.

<sub> Documentation is available at [inbox-docs](https://centerfordigitalhumanities.github.io/inbox-docs/).</sub>


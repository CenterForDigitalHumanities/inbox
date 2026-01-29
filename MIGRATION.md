# Migration from Java to Node.js

This document describes the migration of the Rerum Inbox from a Java/JAX-RS application to a Node.js/Express application.

## What Changed

### Technology Stack
- **Before**: Java 8+, JAX-RS (Java API for RESTful Web Services), Tomcat
- **After**: Node.js 18+, Express.js, PM2 process manager

### Build & Deployment
- **Before**: Java build tools (Ant/Maven), WAR deployment to Tomcat
- **After**: npm for dependency management, direct Node.js execution with PM2

### CI/CD
- **Before**: No automated CI/CD
- **After**: GitHub Actions workflow with automated testing and deployment

## What Stayed the Same

### API Endpoints
All API endpoints maintain the same behavior:
- `GET /messages` - List messages with filtering
- `POST /messages` - Create new announcements
- `GET /id/:noteId` - Retrieve specific message
- `PUT /messages` - Returns 405 Method Not Allowed
- `DELETE /messages` - Returns 405 Method Not Allowed

### Data Storage
- Still uses Firebase Realtime Database at `rerum-inbox.firebaseio.com/messages`
- No changes to data structure or storage

### Validation Rules
- Announcements must not have an existing `@id`
- Announcements must have a `motivation` property
- `@context` is added if missing (defaults to `http://www.w3.org/ns/ldp`)
- Timestamp is automatically added as `published`

### Response Format
The API responses maintain the same JSON-LD structure:
```json
{
  "@context": "http://www.w3.org/ns/ldp",
  "@type": "ldp:Container",
  "@id": "http://inbox.rerum.io/messages",
  "contains": [...]
}
```

## Breaking Changes

### Timestamp Format
The timestamp format for the `published` field has been maintained for backward compatibility using `.toString()`:
- **Format**: "Wed Jan 29 2024 23:00:00 GMT+0000 (UTC)"

This maintains compatibility with existing clients.

## Benefits of the Migration

### Simplified Development
- Single language (JavaScript) for both server and client code
- Simpler dependency management with npm
- Faster development iteration with hot reload

### Easier Deployment
- No need for Tomcat or application server
- Simpler process management with PM2
- Smaller resource footprint
- Faster startup times

### Better DevOps
- Automated CI/CD with GitHub Actions
- Built-in health check endpoint
- Easier monitoring and logging with PM2
- More straightforward scaling options

### Modern Tooling
- Access to the vast npm ecosystem
- Modern async/await patterns
- Better CORS support out of the box
- Active community and frequent updates

## Migration Checklist for Administrators

### Pre-Migration
- [ ] Back up Firebase data
- [ ] Document current Apache/Tomcat configuration
- [ ] Note any custom environment variables
- [ ] Test current API endpoints and save sample responses

### Server Setup
- [ ] Install Node.js 18+ on the server
- [ ] Install PM2: `npm install -g pm2`
- [ ] Create deployment directory (e.g., `/opt/inbox`)
- [ ] Set up SSH keys for GitHub Actions deployment

### GitHub Repository
- [ ] Add required secrets to GitHub repository:
  - [ ] SSH_PRIVATE_KEY
  - [ ] SSH_HOST
  - [ ] SSH_USER
  - [ ] DEPLOY_PATH
- [ ] Verify GitHub Actions workflow runs successfully

### Apache Configuration
- [ ] Update proxy configuration to point to Node.js (port 3000)
- [ ] Enable required Apache modules (`proxy`, `proxy_http`, `headers`)
- [ ] Test proxy configuration
- [ ] Reload/restart Apache

### Post-Migration
- [ ] Verify all API endpoints respond correctly
- [ ] Test health check endpoint: `/health`
- [ ] Monitor PM2 logs: `pm2 logs inbox`
- [ ] Check Apache error logs for any issues
- [ ] Test client applications with new endpoint
- [ ] Update any documentation referencing the Java implementation

### Rollback Plan (if needed)
If issues occur, you can rollback by:
1. Stop the Node.js application: `pm2 stop inbox`
2. Restore previous Apache/Tomcat configuration
3. Restart Tomcat with the Java WAR file
4. Update DNS/proxy settings if changed

## Testing the Migration

### Health Check
```bash
curl http://localhost:3000/health
# Expected: {"status":"ok","timestamp":"..."}
```

### Get Messages
```bash
curl http://inbox.rerum.io/messages
# Expected: JSON with @context, @type, @id, and contains array
```

### Create Message
```bash
curl -X POST http://inbox.rerum.io/messages \
  -H "Content-Type: application/json" \
  -d '{
    "@type": "Announce",
    "motivation": "test:migration",
    "object": "http://example.org/test",
    "target": "http://example.org/target"
  }'
# Expected: JSON with @id assigned
```

### Get Specific Message
```bash
curl http://inbox.rerum.io/id/{messageId}
# Expected: JSON with that specific message
```

## Support and Troubleshooting

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed troubleshooting steps.

### Common Issues

**Application won't start**
- Check Node.js version: `node --version` (should be 18+)
- Check PM2 status: `pm2 status`
- View logs: `pm2 logs inbox`

**Apache proxy not working**
- Verify Apache modules: `apache2ctl -M | grep proxy`
- Check Apache error logs: `sudo tail -f /var/log/apache2/error.log`
- Test backend directly: `curl http://localhost:3000/health`

**Firebase connection issues**
- Verify network connectivity
- Check firewall rules
- Ensure no changes to Firebase URL or credentials

## Performance Considerations

The Node.js application is expected to have:
- **Lower memory footprint** (compared to Tomcat + Java)
- **Faster startup time** (~1-2 seconds vs 10-30 seconds)
- **Similar throughput** for typical workloads
- **Better CPU efficiency** for I/O-bound operations

For high-traffic scenarios, consider:
- Running multiple PM2 instances: `pm2 start server.js -i max`
- Using a load balancer (e.g., nginx upstream)
- Enabling PM2 cluster mode for automatic load distribution

## Monitoring

PM2 provides built-in monitoring:
```bash
pm2 monit           # Real-time monitoring
pm2 logs inbox      # View logs
pm2 status          # Process status
pm2 describe inbox  # Detailed info
```

For production, consider integrating with:
- PM2 Plus (official monitoring solution)
- Application Performance Monitoring (APM) tools
- Log aggregation services (e.g., ELK stack, CloudWatch)

## Future Enhancements

Possible improvements to consider:
- Add unit and integration tests
- Implement rate limiting
- Add request logging middleware
- Set up error tracking (e.g., Sentry)
- Add metrics collection (e.g., Prometheus)
- Implement caching layer for frequently accessed messages
- Add OpenAPI/Swagger documentation

## Questions?

For questions or issues related to this migration, please:
1. Check [DEPLOYMENT.md](DEPLOYMENT.md) for configuration help
2. Review the [README.md](readme.md) for API documentation
3. Open an issue on the GitHub repository
4. Contact the Center for Digital Humanities

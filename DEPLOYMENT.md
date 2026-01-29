# Deployment Configuration

## Repository Secrets Required

The following secrets must be configured in GitHub repository settings (Settings > Secrets and variables > Actions):

### Required Secrets

1. **SSH_PRIVATE_KEY**
   - Description: Private SSH key for deploying to the TPEN-Services machine
   - Format: Full SSH private key (RSA or ED25519)
   - Example generation: `ssh-keygen -t ed25519 -C "github-actions@inbox"`
   - The corresponding public key must be added to `~/.ssh/authorized_keys` on the target server

2. **SSH_HOST**
   - Description: Hostname or IP address of the TPEN-Services machine
   - Format: Hostname or IP address
   - Example: `tpen-services.example.com` or `192.168.1.100`

3. **SSH_USER**
   - Description: Username for SSH connection to the TPEN-Services machine
   - Format: Unix username
   - Example: `deploy` or `tpen`

4. **DEPLOY_PATH**
   - Description: Absolute path on the server where the application should be deployed
   - Format: Absolute path
   - Example: `/opt/inbox` or `/home/tpen/apps/inbox`
   - Note: This directory will be created if it doesn't exist

## Server Requirements

The TPEN-Services machine must have:

1. **Node.js** (v18 or later)
   ```bash
   curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
   sudo apt-get install -y nodejs
   ```

2. **PM2** (Process Manager for Node.js)
   ```bash
   sudo npm install -g pm2
   ```

3. **SSH Access**
   - SSH daemon running
   - Public key authentication configured
   - Deploy user has permission to write to DEPLOY_PATH

## Apache Configuration

The application runs on port 3000 by default. Apache should be configured to proxy requests to the Node.js application.

### Apache Proxy Configuration

Add the following configuration to your Apache virtual host configuration (typically in `/etc/apache2/sites-available/`):

```apache
# Enable required modules
# Run: sudo a2enmod proxy proxy_http headers

<VirtualHost *:80>
    ServerName inbox.rerum.io
    
    # Proxy settings
    ProxyPreserveHost On
    ProxyPass /inbox http://localhost:3000
    ProxyPassReverse /inbox http://localhost:3000
    
    # Optional: Add headers
    <Location /inbox>
        Header set Access-Control-Allow-Origin "*"
        Header set Access-Control-Allow-Methods "GET, POST, OPTIONS"
        Header set Access-Control-Allow-Headers "Content-Type"
    </Location>
    
    # Handle OPTIONS requests for CORS
    <Location /inbox>
        <IfModule mod_headers.c>
            Header always set Access-Control-Allow-Origin "*"
            Header always set Access-Control-Allow-Methods "GET, POST, OPTIONS"
            Header always set Access-Control-Allow-Headers "Content-Type"
        </IfModule>
    </Location>
</VirtualHost>
```

### Apache Configuration Changes from Java Application

**Previous Java Configuration:**
- Application ran on Tomcat (typically port 8080)
- Context path was defined in `web/META-INF/context.xml`
- Required Tomcat-specific deployment descriptors

**New Node.js Configuration:**
- Application runs directly on port 3000
- No context.xml or deployment descriptors needed
- Simpler proxy configuration
- Built-in CORS support in application

### Enable Apache Modules

```bash
sudo a2enmod proxy
sudo a2enmod proxy_http
sudo a2enmod headers
sudo systemctl restart apache2
```

### Alternative Nginx Configuration

If using Nginx instead of Apache:

```nginx
server {
    listen 80;
    server_name inbox.rerum.io;

    location /inbox {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        
        # CORS headers
        add_header Access-Control-Allow-Origin * always;
        add_header Access-Control-Allow-Methods "GET, POST, OPTIONS" always;
        add_header Access-Control-Allow-Headers "Content-Type" always;
    }
}
```

## Environment Variables

The application supports the following environment variables:

- **PORT**: Port number for the application (default: 3000)
- **ID_ROOT**: Base URL for generated IDs (default: `http://inbox.rerum.io`)

Configure these in PM2 ecosystem file or systemd service:

### PM2 Ecosystem File

Create `ecosystem.config.js` in the deployment directory:

```javascript
module.exports = {
  apps: [{
    name: 'inbox',
    script: './server.js',
    instances: 1,
    autorestart: true,
    watch: false,
    max_memory_restart: '1G',
    env: {
      NODE_ENV: 'production',
      PORT: 3000,
      ID_ROOT: 'http://inbox.rerum.io'
    }
  }]
};
```

Start with: `pm2 start ecosystem.config.js`

## Manual Deployment Steps

If deploying manually without GitHub Actions:

1. Clone or copy the repository to the server
   ```bash
   cd /opt
   git clone https://github.com/CenterForDigitalHumanities/inbox.git
   cd inbox
   ```

2. Install dependencies
   ```bash
   npm ci --production
   ```

3. Start with PM2
   ```bash
   pm2 start server.js --name inbox
   pm2 save
   pm2 startup  # Configure PM2 to start on boot
   ```

4. Configure Apache/Nginx as described above

5. Restart the web server
   ```bash
   sudo systemctl restart apache2
   # or
   sudo systemctl restart nginx
   ```

## Health Check

The application provides a health check endpoint:

```bash
curl http://localhost:3000/health
```

Response:
```json
{
  "status": "ok",
  "timestamp": "2024-01-29T23:00:00.000Z"
}
```

## Monitoring

View application logs:
```bash
pm2 logs inbox
```

Monitor application status:
```bash
pm2 status
pm2 monit
```

## Troubleshooting

### Application won't start
- Check Node.js version: `node --version` (should be 18+)
- Check for port conflicts: `lsof -i :3000`
- Review logs: `pm2 logs inbox --lines 100`

### Apache proxy not working
- Verify modules are enabled: `apache2ctl -M | grep proxy`
- Check Apache error logs: `sudo tail -f /var/log/apache2/error.log`
- Verify the app is running: `curl http://localhost:3000/health`

### Firebase connection issues
- Verify network connectivity to Firebase
- Check firewall rules
- Review application logs for detailed error messages

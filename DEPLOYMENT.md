# Deployment Configuration

## CI/CD Pipeline

This repository uses GitHub Actions for automated testing and deployment. The workflow is defined in `.github/workflows/nodejs-cicd.yml`.

### Workflow Overview

- **Trigger**: Push to `main` branch or pull requests to `development` branch
- **Test Job**: Runs on `ubuntu-latest`, tests with Node.js 24
- **Deploy Job**: Runs on specified runner after tests pass

### Repository Secrets Required

The following secrets must be configured in GitHub repository settings (Settings > Secrets and variables > Actions):

#### GitHub Actions Runner Secrets (for DEV/PRD deployment)

1. **TPEN_EMAIL_CC** (Optional)
   - Description: Email address to CC on notifications
   - Format: Email address
   - Used by: CI/CD workflow for notifications

#### SSH Deployment Secrets (if using SSH deployment)

If the deploy job uses SSH to deploy to remote servers, configure:

1. **SSH_PRIVATE_KEY**
   - Description: Private SSH key for deploying to the target machine
   - Format: Full SSH private key (RSA or ED25519)
   - Example generation: `ssh-keygen -t ed25519 -C "github-actions@inbox"`
   - The corresponding public key must be added to `~/.ssh/authorized_keys` on the target server

2. **SSH_HOST**
   - Description: Hostname or IP address of the target deployment machine
   - Format: Hostname or IP address
   - Example: `tpen-services.example.com` or `192.168.1.100`

3. **SSH_USER**
   - Description: Username for SSH connection to the target machine
   - Format: Unix username
   - Example: `deploy` or `tpen`

4. **DEPLOY_PATH**
   - Description: Absolute path on the server where the application should be deployed
   - Format: Absolute path
   - Example: `/opt/inbox` or `/home/tpen/apps/inbox`
   - Note: This directory will be created if it doesn't exist

## Environment Variables

The application supports the following environment variables:

- **PORT**: Port number for the application (default: 3000)
- **ID_ROOT**: Base URL for generated IDs (default: `http://inbox.rerum.io`)
- **NODE_ENV**: Runtime environment (development or production, default: production)

### Configure in PM2

Configure environment variables in PM2 ecosystem file or systemd service:

#### PM2 Ecosystem File

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

## Automated Deployment via GitHub Actions

The CI/CD pipeline automatically:

1. **Tests** the application on every push to `main` and pull requests to `development`
2. **Deploys** to the configured runners after tests pass
3. **Validates** the deployment by waiting for the service to be ready

### Setting Up GitHub Actions Runners

To deploy to your DEV and PRD servers, you need to set up self-hosted GitHub Actions runners:

1. **On the DEV server**:
   ```bash
   # Follow GitHub Actions runner setup documentation
   # https://docs.github.com/en/actions/hosting-your-own-runners/managing-self-hosted-runners
   ```

2. **On the PRD server**:
   ```bash
   # Same setup as DEV
   ```

3. **Configure runner labels** for DEV and PRD environments

4. **Update `.github/workflows/nodejs-cicd.yml`** to specify runner labels:
   ```yaml
   deploy-dev:
     runs-on: [self-hosted, dev-runner]
     
   deploy-prd:
     runs-on: [self-hosted, prd-runner]
   ```

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

4. Configure Apache/Nginx as described below

5. Restart the web server
   ```bash
   sudo systemctl restart apache2
   # or
   sudo systemctl restart nginx
   ```

## Server Requirements

The deployment server must have:

1. **Node.js** (v18 or later, v20+ recommended)
   ```bash
   curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
   sudo apt-get install -y nodejs
   ```

2. **PM2** (Process Manager for Node.js)
   ```bash
   sudo npm install -g pm2
   ```

3. **SSH Access** (if using SSH deployment)
   - SSH daemon running
   - Public key authentication configured
   - Deploy user has permission to write to DEPLOY_PATH

## Application Port

The application runs on port 3000 by default. Configure your reverse proxy (Apache or Nginx) to route traffic to this port.

## Apache Proxy Configuration

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
- Deployed via GitHub Actions without self-hosted runners

**New Node.js Configuration:**
- Application runs directly on port 3000
- No context.xml or deployment descriptors needed
- Simpler proxy configuration
- Built-in CORS support in application
- Deployed via GitHub Actions with self-hosted runners on DEV/PRD servers

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

### CI/CD Pipeline Issues
- Check runner status: Repository Settings > Actions > Runners
- Verify runner has Node.js 24 installed
- Review workflow logs in GitHub Actions tab
- Ensure deploy secrets are correctly configured

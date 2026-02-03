export default {
  apps: [
    {
      name: 'inbox',
      script: './server.js',
      instances: 1,
      autorestart: true,
      watch: false,
      max_memory_restart: '1G',
      env: {
        NODE_ENV: 'development',
        PORT: 3000,
        MONGODB_URL: 'mongodb://localhost:27017/inbox',
        MONGODB_COLLECTION: 'messages',
        ID_ROOT: 'http://inbox.rerum.io'
      },
      env_production: {
        NODE_ENV: 'production',
        PORT: 3000,
        MONGODB_URL: 'mongodb://vlcdhimgp01:27017/inbox?readPreference=primary&appname=MongoDB%20Compass&ssl=false',
        MONGODB_COLLECTION: 'messages',
        ID_ROOT: 'http://inbox.rerum.io'
      }
    }
  ]
}

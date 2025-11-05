module.exports = {
    // Node-RED settings for eSante Data Streaming Layer
    
    // UI Configuration
    uiPort: process.env.PORT || 1880,
    uiHost: "0.0.0.0",
    
    // Flow file location
    flowFile: 'flows.json',
    
    // Credential encryption
    credentialSecret: process.env.NODE_RED_CREDENTIAL_SECRET || "esante-secret-key-change-in-production",
    
    // Admin UI
    httpAdminRoot: '/admin',
    httpNodeRoot: '/',
    
    // Security - disable editor auth for development (enable in production)
    adminAuth: false,
    
    // Logging
    logging: {
        console: {
            level: "info",
            metrics: false,
            audit: false
        }
    },
    
    // Editor theme
    editorTheme: {
        projects: {
            enabled: false
        },
        header: {
            title: "eSante - Data Streaming Layer",
            image: null
        }
    },
    
    // Function node settings
    functionGlobalContext: {
        // Global variables accessible in function nodes
    },
    
    // Export settings for UI nodes
    exportGlobalContextKeys: false
}

const express = require('express');
const multer = require('multer');
const cors = require('cors');
const { S3Client, PutObjectCommand } = require('@aws-sdk/client-s3');
const { v4: uuidv4 } = require('uuid');
require('dotenv').config();

// Initialize Express app
const app = express();
app.use(cors());
app.use(express.json());

// Cloudflare R2 Configuration
const s3 = new S3Client({
    region: 'auto',
    endpoint: `https://${process.env.R2_ACCOUNT_ID}.r2.cloudflarestorage.com`,
    credentials: {
        accessKeyId: process.env.R2_ACCESS_KEY_ID || '',
        secretAccessKey: process.env.R2_SECRET_ACCESS_KEY || '',
    },
});

// Configure multer for memory storage
const upload = multer({ 
    storage: multer.memoryStorage(),
    limits: { fileSize: 100 * 1024 * 1024 }, // 100MB limit for videos and photos
});

// --- API ROUTES ---

// Health Check Endpoint
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', message: 'Backend server is running properly with R2!' });
});

// Media Upload Endpoint (Video/Photo)
app.post('/api/upload', upload.single('media'), async (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No media file provided.' });
    }
    
    try {
        const fileExtension = req.file.originalname.split('.').pop();
        const fileName = `${uuidv4()}.${fileExtension}`;
        
        const command = new PutObjectCommand({
            Bucket: process.env.R2_BUCKET_NAME,
            Key: fileName,
            Body: req.file.buffer,
            ContentType: req.file.mimetype,
        });

        await s3.send(command);
        
        const bucketDomain = process.env.R2_CUSTOM_DOMAIN || `https://pub-${process.env.R2_ACCOUNT_ID}.r2.dev`;
        const fileUrl = `${bucketDomain}/${fileName}`;
        
        res.status(201).json({
            message: 'Media uploaded successfully to R2',
            media: {
                filename: fileName,
                url: fileUrl,
                mimetype: req.file.mimetype,
                uploadTime: new Date().toISOString()
            }
        });
    } catch (error) {
        console.error('R2 Upload Error:', error);
        res.status(500).json({ error: 'Failed to upload media to Cloudflare R2 server.' });
    }
});

// Start the server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is successfully running on port ${PORT}`);
    console.log(`Ready for Render deployment with Cloudflare R2!`);
});

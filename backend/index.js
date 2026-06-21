const express = require('express');
const multer = require('multer');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
require('dotenv').config();

// Initialize Express app
const app = express();
app.use(cors());
app.use(express.json());

// Set up storage directory for videos
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

// Configure multer for video file uploads
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, uploadDir);
    },
    filename: function (req, file, cb) {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        const ext = path.extname(file.originalname);
        cb(null, file.fieldname + '-' + uniqueSuffix + ext);
    }
});

const upload = multer({ 
    storage: storage,
    limits: { fileSize: 100 * 1024 * 1024 }, // 100MB limit for videos
    fileFilter: (req, file, cb) => {
        // Only accept video files
        if (file.mimetype.startsWith('video/')) {
            cb(null, true);
        } else {
            cb(new Error('Only video files are allowed!'), false);
        }
    }
});

// --- API ROUTES ---

// Health Check Endpoint
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', message: 'Backend server is running properly!' });
});

// Video Upload Endpoint
app.post('/api/videos/upload', upload.single('video'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No video file provided.' });
    }
    
    // Define the URL where the video can be accessed
    const videoUrl = `/uploads/${req.file.filename}`;
    
    // In a real application, you would save video metadata (url, user_id, title) to your database here.
    
    res.status(201).json({
        message: 'Video uploaded successfully',
        video: {
            filename: req.file.filename,
            url: videoUrl,
            size: req.file.size,
            mimetype: req.file.mimetype,
            uploadTime: new Date().toISOString()
        }
    });
});

// Get all uploaded videos list
app.get('/api/videos', (req, res) => {
    fs.readdir(uploadDir, (err, files) => {
        if (err) {
            return res.status(500).json({ error: 'Failed to retrieve videos' });
        }
        
        const videos = files.map(file => ({
            filename: file,
            url: `/uploads/${file}`
        }));
        
        res.json({ videos });
    });
});

// Serve uploaded videos statically so the frontend can play them
app.use('/uploads', express.static(uploadDir));

// Start the server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is successfully running on port ${PORT}`);
    console.log(`Ready for Render deployment!`);
});

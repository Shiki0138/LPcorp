/**
 * Advanced File Storage System
 * Handles file uploads, processing, optimization, and management
 */

import { promises as fs } from 'fs';
import path from 'path';
import sharp from 'sharp';
import { prisma } from './database';
import { redis } from './redis';
import { v4 as uuidv4 } from 'uuid';
import mimeTypes from 'mime-types';
import type { FileType, AccessLevel } from '@prisma/client';

export interface FileUploadOptions {
  projectId?: string;
  category?: string;
  accessLevel?: AccessLevel;
  processImages?: boolean;
  generateThumbnails?: boolean;
  optimizeImages?: boolean;
  allowedTypes?: string[];
  maxSize?: number; // bytes
  quality?: number; // for image compression
}

export interface ProcessedFile {
  id: string;
  filename: string;
  originalName: string;
  mimeType: string;
  size: number;
  path: string;
  url: string;
  fileType: FileType;
  width?: number;
  height?: number;
  format?: string;
  thumbnailPath?: string;
  thumbnailUrl?: string;
  optimized: boolean;
  processed: boolean;
}

export interface FileProcessingResult {
  success: boolean;
  file?: ProcessedFile;
  error?: string;
  warnings?: string[];
}

export class FileStorageService {
  private static readonly UPLOAD_DIR = process.env.UPLOAD_DIR || './uploads';
  private static readonly PUBLIC_URL = process.env.PUBLIC_URL || 'http://localhost:3000';
  private static readonly MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
  private static readonly ALLOWED_IMAGE_TYPES = [
    'image/jpeg',
    'image/png',
    'image/webp',
    'image/gif',
    'image/svg+xml',
  ];
  private static readonly ALLOWED_DOCUMENT_TYPES = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain',
  ];
  private static readonly THUMBNAIL_SIZES = [
    { width: 150, height: 150, suffix: 'thumb' },
    { width: 300, height: 300, suffix: 'small' },
    { width: 600, height: 600, suffix: 'medium' },
  ];

  /**
   * Initialize storage directories
   */
  static async initialize(): Promise<void> {
    try {
      const directories = [
        this.UPLOAD_DIR,
        path.join(this.UPLOAD_DIR, 'projects'),
        path.join(this.UPLOAD_DIR, 'assets'),
        path.join(this.UPLOAD_DIR, 'thumbnails'),
        path.join(this.UPLOAD_DIR, 'temp'),
      ];

      for (const dir of directories) {
        await fs.mkdir(dir, { recursive: true });
      }

      console.log('File storage initialized');
    } catch (error) {
      console.error('Failed to initialize file storage:', error);
      throw new Error('File storage initialization failed');
    }
  }

  /**
   * Upload and process a single file
   */
  static async uploadFile(
    fileBuffer: Buffer,
    originalName: string,
    options: FileUploadOptions = {}
  ): Promise<FileProcessingResult> {
    try {
      // Validate file
      const validation = await this.validateFile(fileBuffer, originalName, options);
      if (!validation.valid) {
        return {
          success: false,
          error: validation.error,
        };
      }

      // Generate unique filename
      const fileId = uuidv4();
      const extension = path.extname(originalName);
      const filename = `${fileId}${extension}`;
      const mimeType = mimeTypes.lookup(originalName) || 'application/octet-stream';
      const fileType = this.determineFileType(mimeType);

      // Determine storage path
      const relativePath = this.buildFilePath(filename, options.projectId);
      const fullPath = path.join(this.UPLOAD_DIR, relativePath);

      // Ensure directory exists
      await fs.mkdir(path.dirname(fullPath), { recursive: true });

      // Save original file
      await fs.writeFile(fullPath, fileBuffer);

      // Process file based on type
      let processedData: any = {};
      if (fileType === 'IMAGE' && options.processImages !== false) {
        processedData = await this.processImage(fullPath, filename, options);
      }

      // Create database record
      const fileRecord = await prisma.projectFile.create({
        data: {
          projectId: options.projectId,
          filename,
          originalName,
          mimeType,
          size: fileBuffer.length,
          path: relativePath,
          url: `${this.PUBLIC_URL}/uploads/${relativePath}`,
          fileType,
          category: options.category,
          width: processedData.width,
          height: processedData.height,
          format: processedData.format,
          optimized: processedData.optimized || false,
          thumbnailPath: processedData.thumbnailPath,
          processed: true,
          accessLevel: options.accessLevel || 'PRIVATE',
          isPublic: options.accessLevel === 'PUBLIC',
        },
      });

      // Cache file metadata
      await this.cacheFileMetadata(fileRecord.id, fileRecord);

      const result: ProcessedFile = {
        id: fileRecord.id,
        filename: fileRecord.filename,
        originalName: fileRecord.originalName,
        mimeType: fileRecord.mimeType,
        size: fileRecord.size,
        path: fileRecord.path,
        url: fileRecord.url || '',
        fileType: fileRecord.fileType,
        width: fileRecord.width || undefined,
        height: fileRecord.height || undefined,
        format: fileRecord.format || undefined,
        thumbnailPath: fileRecord.thumbnailPath || undefined,
        thumbnailUrl: processedData.thumbnailUrl,
        optimized: fileRecord.optimized,
        processed: fileRecord.processed,
      };

      return {
        success: true,
        file: result,
        warnings: processedData.warnings,
      };
    } catch (error) {
      console.error('File upload error:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'File upload failed',
      };
    }
  }

  /**
   * Upload multiple files
   */
  static async uploadFiles(
    files: Array<{ buffer: Buffer; originalName: string }>,
    options: FileUploadOptions = {}
  ): Promise<FileProcessingResult[]> {
    const results: FileProcessingResult[] = [];

    for (const file of files) {
      const result = await this.uploadFile(file.buffer, file.originalName, options);
      results.push(result);
    }

    return results;
  }

  /**
   * Get file by ID
   */
  static async getFile(fileId: string): Promise<ProcessedFile | null> {
    try {
      // Try cache first
      const cacheKey = `file:${fileId}`;
      const cached = await redis.get(cacheKey);
      if (cached) {
        return JSON.parse(cached);
      }

      // Get from database
      const fileRecord = await prisma.projectFile.findUnique({
        where: { id: fileId },
      });

      if (!fileRecord) {
        return null;
      }

      const file: ProcessedFile = {
        id: fileRecord.id,
        filename: fileRecord.filename,
        originalName: fileRecord.originalName,
        mimeType: fileRecord.mimeType,
        size: fileRecord.size,
        path: fileRecord.path,
        url: fileRecord.url || '',
        fileType: fileRecord.fileType,
        width: fileRecord.width || undefined,
        height: fileRecord.height || undefined,
        format: fileRecord.format || undefined,
        thumbnailPath: fileRecord.thumbnailPath || undefined,
        optimized: fileRecord.optimized,
        processed: fileRecord.processed,
      };

      // Cache for future requests
      await this.cacheFileMetadata(fileId, file);

      return file;
    } catch (error) {
      console.error('Get file error:', error);
      return null;
    }
  }

  /**
   * Delete file
   */
  static async deleteFile(fileId: string): Promise<boolean> {
    try {
      const fileRecord = await prisma.projectFile.findUnique({
        where: { id: fileId },
      });

      if (!fileRecord) {
        return false;
      }

      // Delete physical files
      const fullPath = path.join(this.UPLOAD_DIR, fileRecord.path);
      await fs.unlink(fullPath).catch(() => {}); // Ignore if file doesn't exist

      // Delete thumbnails
      if (fileRecord.thumbnailPath) {
        const thumbnailPath = path.join(this.UPLOAD_DIR, fileRecord.thumbnailPath);
        await fs.unlink(thumbnailPath).catch(() => {});
      }

      // Delete from database
      await prisma.projectFile.delete({
        where: { id: fileId },
      });

      // Remove from cache
      await redis.del(`file:${fileId}`);

      return true;
    } catch (error) {
      console.error('Delete file error:', error);
      return false;
    }
  }

  /**
   * Get files for a project
   */
  static async getProjectFiles(
    projectId: string,
    fileType?: FileType
  ): Promise<ProcessedFile[]> {
    try {
      const where: any = { projectId };
      if (fileType) {
        where.fileType = fileType;
      }

      const files = await prisma.projectFile.findMany({
        where,
        orderBy: { createdAt: 'desc' },
      });

      return files.map(file => ({
        id: file.id,
        filename: file.filename,
        originalName: file.originalName,
        mimeType: file.mimeType,
        size: file.size,
        path: file.path,
        url: file.url || '',
        fileType: file.fileType,
        width: file.width || undefined,
        height: file.height || undefined,
        format: file.format || undefined,
        thumbnailPath: file.thumbnailPath || undefined,
        optimized: file.optimized,
        processed: file.processed,
      }));
    } catch (error) {
      console.error('Get project files error:', error);
      return [];
    }
  }

  /**
   * Optimize image
   */
  static async optimizeImage(
    fileId: string,
    quality = 80
  ): Promise<boolean> {
    try {
      const fileRecord = await prisma.projectFile.findUnique({
        where: { id: fileId },
      });

      if (!fileRecord || fileRecord.fileType !== 'IMAGE') {
        return false;
      }

      const fullPath = path.join(this.UPLOAD_DIR, fileRecord.path);
      const optimizedPath = fullPath.replace(
        path.extname(fullPath),
        `_optimized${path.extname(fullPath)}`
      );

      // Optimize image
      await sharp(fullPath)
        .jpeg({ quality, progressive: true })
        .png({ compressionLevel: 9 })
        .webp({ quality })
        .toFile(optimizedPath);

      // Replace original with optimized
      await fs.rename(optimizedPath, fullPath);

      // Update database
      await prisma.projectFile.update({
        where: { id: fileId },
        data: { optimized: true },
      });

      // Update cache
      await redis.del(`file:${fileId}`);

      return true;
    } catch (error) {
      console.error('Image optimization error:', error);
      return false;
    }
  }

  /**
   * Generate file URL with access control
   */
  static async generateSecureUrl(
    fileId: string,
    expiresIn = 3600
  ): Promise<string | null> {
    try {
      const file = await this.getFile(fileId);
      if (!file) {
        return null;
      }

      // For public files, return direct URL
      if (file.url) {
        return file.url;
      }

      // Generate temporary access token
      const token = uuidv4();
      const cacheKey = `file_token:${token}`;
      
      await redis.setex(cacheKey, expiresIn, fileId);

      return `${this.PUBLIC_URL}/api/files/secure/${token}`;
    } catch (error) {
      console.error('Generate secure URL error:', error);
      return null;
    }
  }

  /**
   * Private helper methods
   */
  private static async validateFile(
    fileBuffer: Buffer,
    originalName: string,
    options: FileUploadOptions
  ): Promise<{ valid: boolean; error?: string }> {
    // Check file size
    const maxSize = options.maxSize || this.MAX_FILE_SIZE;
    if (fileBuffer.length > maxSize) {
      return {
        valid: false,
        error: `File size exceeds maximum allowed size of ${maxSize} bytes`,
      };
    }

    // Check file type
    const mimeType = mimeTypes.lookup(originalName);
    if (!mimeType) {
      return {
        valid: false,
        error: 'Invalid file type',
      };
    }

    const allowedTypes = options.allowedTypes || [
      ...this.ALLOWED_IMAGE_TYPES,
      ...this.ALLOWED_DOCUMENT_TYPES,
    ];

    if (!allowedTypes.includes(mimeType)) {
      return {
        valid: false,
        error: `File type ${mimeType} is not allowed`,
      };
    }

    return { valid: true };
  }

  private static determineFileType(mimeType: string): FileType {
    if (mimeType.startsWith('image/')) {
      return 'IMAGE';
    } else if (mimeType.startsWith('video/')) {
      return 'VIDEO';
    } else if (mimeType.startsWith('audio/')) {
      return 'AUDIO';
    } else if (
      mimeType.includes('pdf') ||
      mimeType.includes('document') ||
      mimeType.includes('text')
    ) {
      return 'DOCUMENT';
    } else if (
      mimeType.includes('zip') ||
      mimeType.includes('rar') ||
      mimeType.includes('tar')
    ) {
      return 'ARCHIVE';
    }
    return 'OTHER';
  }

  private static buildFilePath(filename: string, projectId?: string): string {
    const date = new Date();
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    
    if (projectId) {
      return `projects/${projectId}/${year}/${month}/${filename}`;
    }
    
    return `assets/${year}/${month}/${filename}`;
  }

  private static async processImage(
    fullPath: string,
    filename: string,
    options: FileUploadOptions
  ): Promise<any> {
    try {
      const warnings: string[] = [];
      const metadata = await sharp(fullPath).metadata();
      
      let optimized = false;
      let thumbnailPath: string | undefined;
      let thumbnailUrl: string | undefined;

      // Optimize image if requested
      if (options.optimizeImages !== false) {
        const quality = options.quality || 80;
        await sharp(fullPath)
          .jpeg({ quality, progressive: true })
          .png({ compressionLevel: 9 })
          .webp({ quality })
          .toFile(fullPath);
        optimized = true;
      }

      // Generate thumbnails if requested
      if (options.generateThumbnails !== false) {
        const thumbnailDir = path.join(this.UPLOAD_DIR, 'thumbnails');
        const fileBasename = path.parse(filename).name;
        
        try {
          const thumbFilename = `${fileBasename}_thumb.webp`;
          thumbnailPath = `thumbnails/${thumbFilename}`;
          const thumbFullPath = path.join(thumbnailDir, thumbFilename);
          
          await sharp(fullPath)
            .resize(300, 300, { fit: 'cover' })
            .webp({ quality: 80 })
            .toFile(thumbFullPath);
            
          thumbnailUrl = `${this.PUBLIC_URL}/uploads/${thumbnailPath}`;
        } catch (error) {
          warnings.push('Failed to generate thumbnail');
        }
      }

      return {
        width: metadata.width,
        height: metadata.height,
        format: metadata.format,
        optimized,
        thumbnailPath,
        thumbnailUrl,
        warnings,
      };
    } catch (error) {
      console.error('Image processing error:', error);
      return {
        warnings: ['Failed to process image'],
      };
    }
  }

  private static async cacheFileMetadata(
    fileId: string,
    metadata: any
  ): Promise<void> {
    try {
      const cacheKey = `file:${fileId}`;
      await redis.setex(cacheKey, 3600, JSON.stringify(metadata)); // 1 hour
    } catch (error) {
      console.error('Cache file metadata error:', error);
    }
  }
}

export default FileStorageService;
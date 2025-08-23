import sharp from 'sharp';
import path from 'path';
import fs from 'fs/promises';
import { v4 as uuidv4 } from 'uuid';
import type { ImageProcessingOptions, ProcessedImage } from '@/types';

const UPLOAD_DIR = process.env.UPLOAD_DIR || './uploads';
const DEFAULT_QUALITY = parseInt(process.env.IMAGE_QUALITY || '80', 10);
const DEFAULT_WIDTH = parseInt(process.env.IMAGE_RESIZE_WIDTH || '1920', 10);
const DEFAULT_HEIGHT = parseInt(process.env.IMAGE_RESIZE_HEIGHT || '1080', 10);
const THUMBNAIL_WIDTH = parseInt(process.env.THUMBNAIL_WIDTH || '300', 10);
const THUMBNAIL_HEIGHT = parseInt(process.env.THUMBNAIL_HEIGHT || '200', 10);

/**
 * Ensures upload directory exists
 */
export async function ensureUploadDirectory(projectId: string): Promise<string> {
  const projectDir = path.join(UPLOAD_DIR, 'projects', projectId);
  await fs.mkdir(projectDir, { recursive: true });
  
  const imagesDir = path.join(projectDir, 'images');
  const thumbnailsDir = path.join(projectDir, 'thumbnails');
  
  await Promise.all([
    fs.mkdir(imagesDir, { recursive: true }),
    fs.mkdir(thumbnailsDir, { recursive: true }),
  ]);
  
  return projectDir;
}

/**
 * Processes image file with optimization and thumbnail generation
 */
export async function processImage(
  inputPath: string,
  projectId: string,
  options: ImageProcessingOptions = {}
): Promise<ProcessedImage> {
  const {
    width = DEFAULT_WIDTH,
    height = DEFAULT_HEIGHT,
    quality = DEFAULT_QUALITY,
    format = 'webp',
    createThumbnail = true,
    thumbnailWidth = THUMBNAIL_WIDTH,
    thumbnailHeight = THUMBNAIL_HEIGHT,
  } = options;

  try {
    // Ensure project directory exists
    const projectDir = await ensureUploadDirectory(projectId);
    
    // Generate unique filename
    const fileId = uuidv4();
    const processedFilename = `${fileId}.${format}`;
    const thumbnailFilename = `${fileId}_thumb.${format}`;
    
    const processedPath = path.join(projectDir, 'images', processedFilename);
    const thumbnailPath = createThumbnail 
      ? path.join(projectDir, 'thumbnails', thumbnailFilename)
      : undefined;

    // Get original image metadata
    const image = sharp(inputPath);
    const metadata = await image.metadata();
    
    if (!metadata.width || !metadata.height) {
      throw new Error('Unable to read image metadata');
    }

    // Process main image
    const processedImage = image
      .resize(width, height, {
        fit: 'inside',
        withoutEnlargement: true,
      })
      .toFormat(format as keyof sharp.FormatEnum, {
        quality,
        progressive: true,
        mozjpeg: format === 'jpeg',
      });

    await processedImage.toFile(processedPath);

    // Generate thumbnail if requested
    if (createThumbnail && thumbnailPath) {
      await image
        .resize(thumbnailWidth, thumbnailHeight, {
          fit: 'cover',
          position: 'center',
        })
        .toFormat(format as keyof sharp.FormatEnum, {
          quality: Math.max(quality - 10, 60),
        })
        .toFile(thumbnailPath);
    }

    // Get processed image info
    const processedInfo = await sharp(processedPath).metadata();
    const processedStats = await fs.stat(processedPath);

    return {
      originalPath: inputPath,
      processedPath: path.relative(UPLOAD_DIR, processedPath),
      thumbnailPath: thumbnailPath ? path.relative(UPLOAD_DIR, thumbnailPath) : undefined,
      width: processedInfo.width || 0,
      height: processedInfo.height || 0,
      size: processedStats.size,
      format,
      metadata: {
        originalWidth: metadata.width,
        originalHeight: metadata.height,
        originalFormat: metadata.format,
        originalSize: (await fs.stat(inputPath)).size,
        compression: format,
        quality,
      },
    };
  } catch (error) {
    throw new Error(`Image processing failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
  }
}

/**
 * Automatically categorizes images using basic analysis
 */
export async function categorizeImage(imagePath: string): Promise<string> {
  try {
    const image = sharp(imagePath);
    const metadata = await image.metadata();
    const stats = await image.stats();
    
    // Basic categorization logic based on image properties
    const aspectRatio = (metadata.width || 1) / (metadata.height || 1);
    const isLandscape = aspectRatio > 1.3;
    const isPortrait = aspectRatio < 0.7;
    const isSquare = aspectRatio >= 0.9 && aspectRatio <= 1.1;
    
    // Analyze color composition
    const dominantColor = stats.dominant;
    const isColorful = stats.channels.some(channel => channel.mean > 200);
    const isLowSaturation = stats.channels.every(channel => 
      Math.abs(channel.mean - stats.channels[0]!.mean) < 30
    );

    // Categorization logic
    if (metadata.width && metadata.width > 1200 && isLandscape) {
      return 'banner';
    } else if (isSquare && metadata.width && metadata.width < 500) {
      return 'icon';
    } else if (isPortrait) {
      return 'portrait';
    } else if (isLowSaturation) {
      return 'document';
    } else if (isColorful) {
      return 'graphic';
    } else if (isLandscape) {
      return 'landscape';
    }
    
    return 'general';
  } catch (error) {
    console.warn('Image categorization failed:', error);
    return 'uncategorized';
  }
}

/**
 * Validates image format and converts if necessary
 */
export function getSupportedFormat(originalFormat: string): 'jpeg' | 'png' | 'webp' {
  const format = originalFormat.toLowerCase();
  
  if (format === 'jpg' || format === 'jpeg') {
    return 'jpeg';
  } else if (format === 'png') {
    return 'png';
  } else if (format === 'webp') {
    return 'webp';
  }
  
  // Default to JPEG for unsupported formats
  return 'jpeg';
}

/**
 * Cleanup temporary files
 */
export async function cleanupTempFiles(filePaths: string[]): Promise<void> {
  await Promise.allSettled(
    filePaths.map(async (filePath) => {
      try {
        await fs.unlink(filePath);
      } catch (error) {
        console.warn(`Failed to cleanup temp file ${filePath}:`, error);
      }
    })
  );
}

/**
 * Get image information without processing
 */
export async function getImageInfo(imagePath: string): Promise<{
  width: number;
  height: number;
  format: string;
  size: number;
  aspectRatio: number;
}> {
  const image = sharp(imagePath);
  const metadata = await image.metadata();
  const stats = await fs.stat(imagePath);
  
  return {
    width: metadata.width || 0,
    height: metadata.height || 0,
    format: metadata.format || 'unknown',
    size: stats.size,
    aspectRatio: (metadata.width || 1) / (metadata.height || 1),
  };
}
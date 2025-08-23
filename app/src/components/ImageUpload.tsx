'use client';

import React, { useState, useCallback, useRef } from 'react';
import { Upload, X, FileText, Image, File, Loader2, AlertCircle, CheckCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';

interface ImageUploadProps {
  onFilesChange: (files: File[]) => void;
  maxFiles?: number;
  maxFileSize?: number; // in MB
  acceptedTypes?: string[];
  className?: string;
  multiple?: boolean;
}

interface FileWithPreview extends File {
  preview?: string;
  id: string;
  status: 'uploading' | 'success' | 'error';
  error?: string;
}

export default function ImageUpload({
  onFilesChange,
  maxFiles = 5,
  maxFileSize = 10,
  acceptedTypes = ['image/*', '.pdf', '.doc', '.docx', '.txt'],
  className = '',
  multiple = true
}: ImageUploadProps) {
  const [files, setFiles] = useState<FileWithPreview[]>([]);
  const [isDragOver, setIsDragOver] = useState(false);
  const [uploadError, setUploadError] = useState<string>('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const processFiles = useCallback(async (newFiles: FileList | File[]) => {
    const fileArray = Array.from(newFiles);
    setUploadError('');

    // Validate file count
    if (files.length + fileArray.length > maxFiles) {
      setUploadError(`Maximum ${maxFiles} files allowed`);
      return;
    }

    // Process each file
    const processedFiles = await Promise.all(
      fileArray.map(async (file): Promise<FileWithPreview> => {
        const fileWithPreview: FileWithPreview = {
          ...file,
          id: `${Date.now()}-${Math.random()}`,
          status: 'uploading'
        };

        // Validate file size
        if (file.size > maxFileSize * 1024 * 1024) {
          fileWithPreview.status = 'error';
          fileWithPreview.error = `File size exceeds ${maxFileSize}MB`;
          return fileWithPreview;
        }

        // Validate file type
        const isValidType = acceptedTypes.some(type => {
          if (type.includes('*')) {
            const baseType = type.split('/')[0];
            return file.type.startsWith(baseType);
          }
          return file.type === type || file.name.toLowerCase().endsWith(type.replace('.', ''));
        });

        if (!isValidType) {
          fileWithPreview.status = 'error';
          fileWithPreview.error = 'Invalid file type';
          return fileWithPreview;
        }

        // Create preview for images
        if (file.type.startsWith('image/')) {
          try {
            fileWithPreview.preview = await createImagePreview(file);
          } catch (error) {
            console.error('Error creating preview:', error);
          }
        }

        // Simulate upload process
        setTimeout(() => {
          setFiles(prev => 
            prev.map(f => 
              f.id === fileWithPreview.id 
                ? { ...f, status: 'success' }
                : f
            )
          );
        }, 1000 + Math.random() * 2000);

        return fileWithPreview;
      })
    );

    const newFilesState = [...files, ...processedFiles];
    setFiles(newFilesState);
    
    // Update parent component with successful files only
    const successfulFiles = newFilesState
      .filter(f => f.status === 'success')
      .map(f => new File([f], f.name, { type: f.type }));
    
    onFilesChange(successfulFiles);
  }, [files, maxFiles, maxFileSize, acceptedTypes, onFilesChange]);

  const createImagePreview = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        if (e.target?.result) {
          resolve(e.target.result as string);
        } else {
          reject(new Error('Failed to read file'));
        }
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  };

  const removeFile = (fileId: string) => {
    const newFiles = files.filter(f => f.id !== fileId);
    setFiles(newFiles);
    
    const successfulFiles = newFiles
      .filter(f => f.status === 'success')
      .map(f => new File([f], f.name, { type: f.type }));
    
    onFilesChange(successfulFiles);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    
    if (e.dataTransfer.files) {
      processFiles(e.dataTransfer.files);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      processFiles(e.target.files);
    }
  };

  const openFileDialog = () => {
    fileInputRef.current?.click();
  };

  const getFileIcon = (file: FileWithPreview) => {
    if (file.type.startsWith('image/')) {
      return <Image className="w-6 h-6" />;
    } else if (file.type.includes('pdf')) {
      return <FileText className="w-6 h-6 text-red-600" />;
    } else if (file.type.includes('doc')) {
      return <FileText className="w-6 h-6 text-blue-600" />;
    } else {
      return <File className="w-6 h-6" />;
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Upload Error */}
      {uploadError && (
        <Alert className="border-red-200 bg-red-50">
          <AlertCircle className="h-4 w-4 text-red-600" />
          <AlertDescription className="text-red-800">
            {uploadError}
          </AlertDescription>
        </Alert>
      )}

      {/* Drop Zone */}
      <div
        className={`
          border-2 border-dashed rounded-lg p-8 text-center transition-all duration-200
          ${isDragOver 
            ? 'border-blue-500 bg-blue-50' 
            : 'border-gray-300 hover:border-gray-400'
          }
          ${files.length >= maxFiles ? 'opacity-50 pointer-events-none' : 'cursor-pointer'}
        `}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={openFileDialog}
      >
        <input
          ref={fileInputRef}
          type="file"
          multiple={multiple}
          accept={acceptedTypes.join(',')}
          onChange={handleFileSelect}
          className="hidden"
          disabled={files.length >= maxFiles}
        />

        <div className="space-y-4">
          <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center">
            <Upload className={`w-8 h-8 ${isDragOver ? 'text-blue-600' : 'text-gray-400'}`} />
          </div>
          
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              {isDragOver ? 'Drop files here' : 'Upload Files'}
            </h3>
            <p className="text-sm text-gray-600 mb-4">
              Drag and drop files here, or click to browse
            </p>
            
            <div className="text-xs text-gray-500 space-y-1">
              <p>Supported formats: Images, PDF, DOC, DOCX, TXT</p>
              <p>Maximum file size: {maxFileSize}MB</p>
              <p>Maximum files: {maxFiles}</p>
            </div>
          </div>

          <Button 
            type="button" 
            variant="outline" 
            size="sm"
            disabled={files.length >= maxFiles}
            onClick={(e) => {
              e.stopPropagation();
              openFileDialog();
            }}
          >
            Choose Files
          </Button>
        </div>
      </div>

      {/* File List */}
      {files.length > 0 && (
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-gray-900">
            Uploaded Files ({files.length}/{maxFiles})
          </h4>
          
          <div className="space-y-2">
            {files.map((file) => (
              <div
                key={file.id}
                className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg border"
              >
                {/* File Preview/Icon */}
                <div className="flex-shrink-0 w-12 h-12 rounded-lg overflow-hidden bg-white border">
                  {file.preview ? (
                    <img
                      src={file.preview}
                      alt={file.name}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center">
                      {getFileIcon(file)}
                    </div>
                  )}
                </div>

                {/* File Info */}
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">
                    {file.name}
                  </p>
                  <p className="text-xs text-gray-500">
                    {formatFileSize(file.size)}
                  </p>
                  
                  {/* Status */}
                  <div className="flex items-center space-x-2 mt-1">
                    {file.status === 'uploading' && (
                      <>
                        <Loader2 className="w-3 h-3 animate-spin text-blue-600" />
                        <span className="text-xs text-blue-600">Uploading...</span>
                      </>
                    )}
                    {file.status === 'success' && (
                      <>
                        <CheckCircle className="w-3 h-3 text-green-600" />
                        <span className="text-xs text-green-600">Uploaded</span>
                      </>
                    )}
                    {file.status === 'error' && (
                      <>
                        <AlertCircle className="w-3 h-3 text-red-600" />
                        <span className="text-xs text-red-600">{file.error}</span>
                      </>
                    )}
                  </div>
                </div>

                {/* Remove Button */}
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => removeFile(file.id)}
                  className="flex-shrink-0 w-8 h-8 p-0"
                >
                  <X className="w-4 h-4" />
                </Button>
              </div>
            ))}
          </div>

          {/* Upload Progress Summary */}
          <div className="flex items-center justify-between text-xs text-gray-500 pt-2 border-t">
            <span>
              {files.filter(f => f.status === 'success').length} of {files.length} files uploaded
            </span>
            <span>
              {files.filter(f => f.status === 'error').length > 0 && 
                `${files.filter(f => f.status === 'error').length} failed`
              }
            </span>
          </div>
        </div>
      )}

      {/* Usage Guidelines */}
      {files.length === 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <h4 className="text-sm font-medium text-blue-900 mb-2">
            File Upload Guidelines
          </h4>
          <ul className="text-xs text-blue-800 space-y-1">
            <li>• Upload reference images, wireframes, or design inspiration</li>
            <li>• Include any documents with detailed requirements</li>
            <li>• Brand guidelines or style references are helpful</li>
            <li>• Screenshots of existing systems or competitors</li>
          </ul>
        </div>
      )}
    </div>
  );
}
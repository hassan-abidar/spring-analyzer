import React, { useState, useRef, DragEvent } from 'react';
import { Upload, FileArchive, AlertCircle } from 'lucide-react';
import './FileUpload.css';

interface FileUploadProps {
  onFileSelect: (file: File) => void;
  accept?: string;
  maxSize?: number;
  disabled?: boolean;
}

export function FileUpload({ 
  onFileSelect, 
  accept = '.zip', 
  maxSize = 500 * 1024 * 1024, // 500MB limit
  disabled = false 
}: FileUploadProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const validateFile = (file: File): boolean => {
    setError(null);
    
    if (!file.name.toLowerCase().endsWith('.zip')) {
      setError('Only ZIP files are allowed');
      return false;
    }
    
    if (file.size > maxSize) {
      setError(`File size exceeds ${Math.round(maxSize / 1024 / 1024)}MB limit`);
      return false;
    }
    
    return true;
  };

  const handleFile = (file: File) => {
    if (validateFile(file)) {
      setSelectedFile(file);
      onFileSelect(file);
    }
  };

  const handleDrop = (e: DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    
    if (disabled) return;
    
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  };

  const handleDragOver = (e: DragEvent) => {
    e.preventDefault();
    if (!disabled) setIsDragging(true);
  };

  const handleDragLeave = () => setIsDragging(false);

  const handleClick = () => {
    if (!disabled) inputRef.current?.click();
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
  };

  const formatSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1024 / 1024).toFixed(1) + ' MB';
  };

  return (
    <div className="file-upload-container">
      <div
        className={`file-upload-zone ${isDragging ? 'dragging' : ''} ${disabled ? 'disabled' : ''}`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onClick={handleClick}
      >
        <input
          ref={inputRef}
          type="file"
          accept={accept}
          onChange={handleChange}
          disabled={disabled}
          hidden
        />
        
        <div className="upload-icon">
          {selectedFile ? <FileArchive size={40} /> : <Upload size={40} />}
        </div>
        
        {selectedFile ? (
          <div className="selected-file">
            <span className="file-name">{selectedFile.name}</span>
            <span className="file-size">{formatSize(selectedFile.size)}</span>
          </div>
        ) : (
          <div className="upload-text">
            <p className="upload-primary">Drop your ZIP file here</p>
            <p className="upload-secondary">or click to browse</p>
          </div>
        )}
      </div>
      
      {error && (
        <p className="upload-error">
          <AlertCircle size={14} />
          {error}
        </p>
      )}
    </div>
  );
}

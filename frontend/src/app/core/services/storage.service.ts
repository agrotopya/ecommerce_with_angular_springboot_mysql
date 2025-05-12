// src/app/core/services/storage.service.ts
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root',
})
export class StorageService {
  private platformId = inject(PLATFORM_ID);
  public readonly isBrowser: boolean = isPlatformBrowser(this.platformId);

  constructor() {
    // Constructor boş kalabilir veya platformId burada da inject edilebilir.
    // isBrowser zaten sınıf özelliği olarak tanımlandı.
  }

  setItem(key: string, value: any): void {
    if (this.isBrowser) {
      if (value === undefined) {
        console.error(`StorageService - Attempted to set undefined value for key "${key}".`);
        return;
      }
      try {
        localStorage.setItem(key, JSON.stringify(value));
      } catch (error) {
        console.error(`StorageService - setItem Error for key "${key}":`, error);
      }
    }
  }

  getItem<T>(key: string): T | null {
    if (this.isBrowser) {
      try {
        const item = localStorage.getItem(key);
        if (!item) return null;

        try {
          return JSON.parse(item) as T;
        } catch (parseError) {
          console.error(`StorageService - Invalid JSON value for key "${key}". Clearing it.`);
          localStorage.removeItem(key);
          return null;
        }
      } catch (error) {
        console.error(`StorageService - getItem Error for key "${key}":`, error);
        return null;
      }
    }
    return null;
  }

  removeItem(key: string): void {
    if (this.isBrowser) {
      try {
        localStorage.removeItem(key);
      } catch (error) {
        console.error(`StorageService - removeItem Error for key "${key}":`, error);
      }
    }
  }

  clear(): void {
    if (this.isBrowser) {
      try {
        localStorage.clear();
      } catch (error) {
        console.error('StorageService - clear Error:', error);
      }
    }
  }
}

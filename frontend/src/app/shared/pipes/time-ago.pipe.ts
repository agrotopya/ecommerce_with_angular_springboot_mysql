// src/app/shared/pipes/time-ago.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timeAgo',
  standalone: true,
})
export class TimeAgoPipe implements PipeTransform {
  transform(value: string | Date | null | undefined): string {
    if (!value) {
      return '';
    }

    let dateValue: Date;
    if (typeof value === 'string') {
      dateValue = new Date(value);
    } else if (value instanceof Date) {
      dateValue = value;
    } else {
      return ''; // Geçersiz değer için boş string
    }

    if (isNaN(dateValue.getTime())) {
      return 'Invalid date'; // Geçersiz tarih formatı için
    }

    const now = new Date();
    const seconds = Math.round(Math.abs((now.getTime() - dateValue.getTime()) / 1000));

    const intervals: { [key: string]: number } = {
      year: 31536000,
      month: 2592000,
      week: 604800,
      day: 86400,
      hour: 3600,
      minute: 60,
      second: 1,
    };

    let counter;
    for (const i in intervals) {
      counter = Math.floor(seconds / intervals[i]);
      if (counter > 0) {
        if (counter === 1) {
          return counter + ' ' + i + ' ago'; // singular (1 day ago)
        } else {
          return counter + ' ' + i + 's ago'; // plural (2 days ago)
        }
      }
    }
    return 'just now';
  }
}

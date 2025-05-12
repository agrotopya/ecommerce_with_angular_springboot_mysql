// src/app/shared/pipes/nl2br.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'nl2br',
  standalone: true,
})
export class Nl2brPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    return value.replace(/\n/g, '<br />');
  }
}
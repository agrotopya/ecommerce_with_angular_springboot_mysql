// src/app/core/services/api.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  get<T>(path: string, params: HttpParams = new HttpParams()): Observable<T> {
    const url = `${this.apiUrl}${path}`;
    console.log(`ApiService: GET request to ${url} with params:`, params.toString());
    return this.http.get<T>(url, { params }).pipe(
      tap({
        next: (response) => console.log(`ApiService: GET response from ${url}:`, response),
        error: (error) => console.error(`ApiService: GET error from ${url}:`, error)
      })
    );
  }

  post<T>(path: string, body: object = {}): Observable<T> {
    const url = `${this.apiUrl}${path}`;
    console.log(`ApiService: POST request to ${url} with body:`, body);
    return this.http.post<T>(url, body).pipe(
      tap({
        next: (response) => console.log(`ApiService: POST response from ${url}:`, response),
        error: (error) => console.error(`ApiService: POST error from ${url}:`, error)
      })
    );
  }

  put<T>(path: string, body: object = {}): Observable<T> {
    const url = `${this.apiUrl}${path}`;
    console.log(`ApiService: PUT request to ${url} with body:`, body);
    return this.http.put<T>(url, body).pipe(
      tap({
        next: (response) => console.log(`ApiService: PUT response from ${url}:`, response),
        error: (error) => console.error(`ApiService: PUT error from ${url}:`, error)
      })
    );
  }

  delete<T>(path: string): Observable<T> {
    const url = `${this.apiUrl}${path}`;
    console.log(`ApiService: DELETE request to ${url}`);
    return this.http.delete<T>(url).pipe(
      tap({
        next: (response) => console.log(`ApiService: DELETE response from ${url}:`, response),
        error: (error) => console.error(`ApiService: DELETE error from ${url}:`, error)
      })
    );
  }

  patch<T>(path: string, body: any | null, options?: {
    headers?: HttpHeaders | {
        [header: string]: string | string[];
    };
    observe?: 'body';
    params?: HttpParams | {
        [param: string]: string | string[];
    };
    reportProgress?: boolean;
    responseType?: 'json';
    withCredentials?: boolean;
  }): Observable<T> {
    const url = `${this.apiUrl}${path}`;
    console.log(`ApiService: PATCH request to ${url} with body:`, body, "and options:", options);
    return this.http.patch<T>(url, body, options).pipe(
      tap({
        next: (response) => console.log(`ApiService: PATCH response from ${url}:`, response),
        error: (error) => console.error(`ApiService: PATCH error from ${url}:`, error)
      })
    );
  }
}

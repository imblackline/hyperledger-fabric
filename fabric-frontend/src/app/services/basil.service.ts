import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Basil } from '../models/basil.model';

@Injectable({
  providedIn: 'root'
})
export class BasilService {
  private apiUrl = 'http://localhost:8080/api/basil';

  constructor(private http: HttpClient) { }

  createBasil(id: string, country: string): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}?id=${id}&country=${country}`, {});
  }

  getBasil(id: string): Observable<string> {
    return this.http.get<string>(`${this.apiUrl}/${id}`);
  }
}

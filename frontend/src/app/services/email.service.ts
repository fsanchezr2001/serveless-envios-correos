import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { SendEmailRequest } from '../models/send-email-request';
import { SendEmailResponse } from '../models/send-email-response';

@Injectable({ providedIn: 'root' })
export class EmailService {
  private readonly http = inject(HttpClient);

  sendEmail(payload: SendEmailRequest): Observable<SendEmailResponse> {
    return this.http.post<SendEmailResponse>(`${environment.apiUrl}/emails`, payload);
  }
}

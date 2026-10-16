import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { EmailService } from '../../services/email.service';

@Component({
  selector: 'app-email-form',
  imports: [ReactiveFormsModule],
  templateUrl: './email-form.html',
  styleUrl: './email-form.css',
})
export class EmailForm {
  private readonly fb = inject(FormBuilder);
  private readonly emailService = inject(EmailService);

  readonly form = this.fb.nonNullable.group({
    to: ['', [Validators.required, Validators.email]],
    toName: [''],
    subject: ['', [Validators.required, Validators.maxLength(150)]],
    message: ['', [Validators.required, Validators.maxLength(5000)]],
  });

  readonly sending = signal(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.sending.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);

    const { to, toName, subject, message } = this.form.getRawValue();

    this.emailService
      .sendEmail({ to, toName, subject, message })
      .pipe(finalize(() => this.sending.set(false)))
      .subscribe({
        next: (response) => {
          this.successMessage.set(response.message || 'Email sent successfully.');
          this.form.reset();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.describeError(error));
        },
      });
  }

  private describeError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Cannot reach the server. Is the backend running?';
    }

    if (error.status === 400) {
      return this.describeValidationError(error);
    }

    if (error.status === 502) {
      return 'The email provider rejected the request.';
    }

    return 'Something went wrong while sending the email. Please try again.';
  }

  private describeValidationError(error: HttpErrorResponse): string {
    const body = error.error as { message?: string; details?: unknown } | null;

    if (body && Array.isArray(body.details) && body.details.length > 0) {
      return (body.details as string[]).join(' ');
    }

    if (body && typeof body.message === 'string' && body.message.length > 0) {
      return body.message;
    }

    return 'The request was rejected. Please check the form and try again.';
  }
}

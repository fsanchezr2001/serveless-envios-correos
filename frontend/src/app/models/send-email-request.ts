export interface SendEmailRequest {
  to: string;
  toName?: string;
  subject: string;
  message: string;
}

import { Component } from '@angular/core';

import { EmailForm } from './components/email-form/email-form';

@Component({
  selector: 'app-root',
  imports: [EmailForm],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}

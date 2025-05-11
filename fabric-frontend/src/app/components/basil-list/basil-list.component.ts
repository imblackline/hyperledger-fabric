import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BasilService } from '../../services/basil.service';

@Component({
  selector: 'app-basil-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './basil-list.component.html',
  styleUrls: ['./basil-list.component.scss']
})
export class BasilListComponent implements OnInit {
  basils: any[] = [];
  newBasil = {
    id: '',
    country: ''
  };
  errorMessage = '';

  constructor(private basilService: BasilService) {}

  ngOnInit(): void {
    // Load initial data
    this.loadBasil('basil001');
  }

  loadBasil(id: string): void {
    this.basilService.getBasil(id).subscribe({
      next: (response) => {
        try {
          const basil = JSON.parse(response);
          this.basils = [basil];
        } catch (e) {
          this.errorMessage = 'Error parsing basil data';
        }
      },
      error: (error) => {
        this.errorMessage = 'Error loading basil: ' + error.message;
      }
    });
  }

  createBasil(): void {
    if (!this.newBasil.id || !this.newBasil.country) {
      this.errorMessage = 'Please fill in all fields';
      return;
    }

    this.basilService.createBasil(this.newBasil.id, this.newBasil.country).subscribe({
      next: (response) => {
        this.loadBasil(this.newBasil.id);
        this.newBasil = { id: '', country: '' };
        this.errorMessage = '';
      },
      error: (error) => {
        this.errorMessage = 'Error creating basil: ' + error.message;
      }
    });
  }
}

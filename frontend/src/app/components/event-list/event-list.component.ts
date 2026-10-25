import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription } from 'rxjs';
import { EventService } from '../../services/event.service';
import { IndustrialEvent } from '../../models/event.model';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatProgressBarModule,
    MatTooltipModule
  ],
  templateUrl: './event-list.component.html',
  styleUrls: ['./event-list.component.scss']
})
export class EventListComponent implements OnInit, OnDestroy {

  displayedColumns: string[] = ['eventId', 'eventType', 'description', 'timestamp', 'createdAt', 'auditStatus'];
  events: IndustrialEvent[] = [];
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  isLoading = false;
  hasError = false;
  errorMessage = '';

  private subscription: Subscription = new Subscription();

  constructor(private eventService: EventService) {}

  ngOnInit(): void {
    this.loadEvents();

    this.subscription.add(
      this.eventService.eventCreated$.subscribe(() => {
        this.pageIndex = 0;
        this.loadEvents();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  loadEvents(): void {
    this.isLoading = true;
    this.hasError = false;
    this.eventService.getEvents(this.pageIndex, this.pageSize, 'desc').subscribe({
      next: (response) => {
        this.events = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.hasError = true;
        this.errorMessage = err?.error?.message || 'Falha ao carregar eventos da telemetria. Verifique a conectividade com o backend.';
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadEvents();
  }
}

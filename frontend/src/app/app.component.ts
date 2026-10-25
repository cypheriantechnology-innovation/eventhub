import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './components/navbar/navbar.component';
import { EventFormComponent } from './components/event-form/event-form.component';
import { EventListComponent } from './components/event-list/event-list.component';
import { MatIconModule } from '@angular/material/icon';
import { EventService } from './services/event.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, NavbarComponent, EventFormComponent, EventListComponent, MatIconModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'EventHub - Gestão de Eventos Industriais';

  totalEvents = 0;
  productionCount = 0;
  qualityCount = 0;
  private sub = new Subscription();

  constructor(private eventService: EventService) {}

  ngOnInit(): void {
    this.loadStats();

    // Listen to new events in real-time and refresh exact stats from DB
    this.sub.add(
      this.eventService.eventCreated$.subscribe(() => {
        this.loadStats();
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  loadStats(): void {
    this.eventService.getEventStats().subscribe({
      next: (stats) => {
        this.totalEvents = stats.total;
        this.productionCount = stats.production;
        this.qualityCount = stats.quality;
      },
      error: () => {
        // Fallback to getEvents if stats endpoint is unavailable
        this.eventService.getEvents(0, 10).subscribe({
          next: (res) => {
            this.totalEvents = res.totalElements;
          }
        });
      }
    });
  }
}

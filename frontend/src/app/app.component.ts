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

    // Listen to new events in real-time
    this.sub.add(
      this.eventService.eventCreated$.subscribe((newEvent) => {
        this.totalEvents++;
        if (newEvent.eventType === 'PRODUCTION') {
          this.productionCount++;
        } else if (newEvent.eventType === 'QUALITY') {
          this.qualityCount++;
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  loadStats(): void {
    this.eventService.getEvents(0, 50).subscribe({
      next: (res) => {
        this.totalEvents = res.totalElements;
        this.productionCount = res.content.filter(e => e.eventType === 'PRODUCTION').length;
        this.qualityCount = res.content.filter(e => e.eventType === 'QUALITY').length;
      },
      error: () => {
        // Fallback or offline graceful handling
      }
    });
  }
}

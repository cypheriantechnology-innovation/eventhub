import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, MatToolbarModule, MatIconModule, MatButtonModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  openSwagger(): void {
    const host = window.location.hostname || 'localhost';
    window.open(`http://${host}:8082/swagger-ui.html`, '_blank');
  }

  openActuator(): void {
    const host = window.location.hostname || 'localhost';
    window.open(`http://${host}:8082/actuator/health`, '_blank');
  }
}

import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuditService } from '../../services/audit.service';
import { AuditLog } from '../../models/event.model';

@Component({
  selector: 'app-audit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule
  ],
  templateUrl: './audit-dialog.component.html',
  styleUrls: ['./audit-dialog.component.scss']
})
export class AuditDialogComponent implements OnInit {

  public auditLogs: AuditLog[] = [];
  public isLoading: boolean = true;
  public errorMessage: string = '';
  public displayedColumns: string[] = ['id', 'action', 'createdAt', 'status'];

  get firstLog(): AuditLog | null {
    return this.auditLogs && this.auditLogs.length > 0 ? this.auditLogs[0] : null;
  }

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { eventId: string },
    private dialogRef: MatDialogRef<AuditDialogComponent>,
    private auditService: AuditService
  ) {}

  ngOnInit(): void {
    this.loadAuditLogs();
  }

  loadAuditLogs(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.auditService.getAuditLogsByEventId(this.data.eventId).subscribe({
      next: (logs) => {
        this.auditLogs = logs;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Não foi possível carregar a trilha de auditoria: ' + (err.error?.message || err.message);
      }
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}

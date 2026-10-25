import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EventService } from '../../services/event.service';
import { CreateEventRequest, EventType } from '../../models/event.model';

@Component({
  selector: 'app-event-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './event-form.component.html',
  styleUrls: ['./event-form.component.scss']
})
export class EventFormComponent {

  eventForm: FormGroup;
  eventTypes: EventType[] = ['PRODUCTION', 'QUALITY'];
  isSubmitting = false;
  isUploading = false;

  constructor(
    private fb: FormBuilder,
    private eventService: EventService,
    private snackBar: MatSnackBar
  ) {
    this.eventForm = this.fb.group({
      eventId: ['', [Validators.required, Validators.maxLength(100)]],
      eventType: ['PRODUCTION', [Validators.required]],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      timestamp: ['']
    });
  }

  onSubmit(): void {
    if (this.eventForm.invalid || this.isSubmitting) {
      this.eventForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const formValue = this.eventForm.value;

    const rawEventId = formValue.eventId ? String(formValue.eventId).trim() : '';
    const rawDescription = formValue.description ? String(formValue.description).trim() : '';

    const request: CreateEventRequest = {
      eventId: rawEventId,
      eventType: formValue.eventType,
      description: rawDescription,
      timestamp: formValue.timestamp ? formValue.timestamp : undefined
    };

    this.eventService.createEvent(request).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.snackBar.open(
          `Evento '${response.eventId}' (${response.eventType}) registrado com sucesso!`,
          'Fechar',
          { duration: 4000, panelClass: ['snack-success'] }
        );
        this.eventService.notifyEventCreated(response);
        this.eventForm.reset({ eventType: 'PRODUCTION' });
      },
      error: (err) => {
        this.isSubmitting = false;
        let errorMessage = 'Falha ao registrar o evento.';
        if (err.status === 409) {
          errorMessage = err.error?.message || `Conflito: o identificador '${request.eventId}' já foi registrado!`;
        } else if (err.status === 400) {
          errorMessage = err.error?.message || 'Dados inválidos. Verifique os campos preenchidos.';
        }

        this.snackBar.open(errorMessage, 'Fechar', {
          duration: 6000,
          panelClass: ['snack-error']
        });
      }
    });
  }

  generateSampleEvent(type: EventType): void {
    const randomNum = Math.floor(1000 + Math.random() * 9000);
    const prefix = type === 'PRODUCTION' ? 'PRD' : 'QLT';
    const sampleDesc = type === 'PRODUCTION'
      ? 'Linha de prensagem iniciou ciclo operacional padrão'
      : 'Sensor térmico identificou oscilação de temperatura fora da tolerância';

    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const localDateTime = `${year}-${month}-${day}T${hours}:${minutes}`;

    this.eventForm.patchValue({
      eventId: `EVT-${prefix}-${randomNum}`,
      eventType: type,
      description: sampleDesc,
      timestamp: localDateTime
    });
  }

  onFileSelected(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      const file = target.files[0];
      this.uploadBatchFile(file);
      target.value = ''; // Reset input to allow selecting same file again
    }
  }

  uploadBatchFile(file: File): void {
    this.isUploading = true;
    this.snackBar.open(`Iniciando ingestão em lote de '${file.name}'...`, undefined, { duration: 2000 });

    this.eventService.uploadBatch(file).subscribe({
      next: (res) => {
        this.isUploading = false;
        const msg = `Lote '${res.filename}': ${res.ingested} ingeridos, ${res.duplicates} duplicados, ${res.errors} erros.`;
        this.snackBar.open(msg, 'Fechar', {
          duration: 7000,
          panelClass: res.errors > 0 ? ['snack-error'] : ['snack-success']
        });
        // Trigger table refresh
        this.eventService.notifyEventCreated({} as any);
      },
      error: (err) => {
        this.isUploading = false;
        const errMsg = err?.error?.message || 'Falha no processamento do arquivo.';
        this.snackBar.open(`Erro no upload: ${errMsg}`, 'Fechar', {
          duration: 6000,
          panelClass: ['snack-error']
        });
      }
    });
  }
}

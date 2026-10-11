import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-user-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './user-edit.component.html',
  styleUrl: './user-edit.component.scss'
})
export class UserEditComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly notificationService = inject(NotificationService);

  userId = signal<number | null>(null);
  user = signal<UserResponse | null>(null);
  isLoading = signal<boolean>(true);
  isSaving = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  editForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
    email: ['', [Validators.required, Validators.email]]
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.userId.set(id);
      this.loadUser(id);
    } else {
      this.errorMessage.set('ID de usuário não fornecido.');
      this.isLoading.set(false);
    }
  }

  loadUser(id: number): void {
    this.isLoading.set(true);
    this.userService.findById(id).subscribe({
      next: (data) => {
        this.user.set(data);
        this.editForm.patchValue({
          name: data.name,
          email: data.email
        });
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Erro ao carregar dados para edição.');
        this.notificationService.error(this.errorMessage()!);
      }
    });
  }

  onSubmit(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const id = this.userId();
    if (!id) return;

    this.isSaving.set(true);
    this.userService.update(id, this.editForm.value).subscribe({
      next: (updated) => {
        this.isSaving.set(false);
        this.notificationService.success(`Usuário "${updated.name}" atualizado com sucesso!`);
        this.router.navigate(['/users', id]);
      },
      error: (err) => {
        this.isSaving.set(false);
        const msg = err.error?.message || 'Falha ao atualizar usuário.';
        this.notificationService.error(msg);
      }
    });
  }
}

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss'
})
export class UserListComponent implements OnInit {
  private readonly userService = inject(UserService);
  readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  users = signal<UserResponse[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);

  // Search & Filters
  searchTerm = signal<string>('');
  roleFilter = signal<string>('ALL');

  // Filtered Users computed signal
  filteredUsers = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const role = this.roleFilter();

    return this.users().filter(user => {
      const matchesSearch =
        !term ||
        user.name.toLowerCase().includes(term) ||
        user.email.toLowerCase().includes(term) ||
        user.id.toString().includes(term);

      const matchesRole =
        role === 'ALL' ||
        user.role.toUpperCase() === role.toUpperCase() ||
        user.role.toUpperCase() === `ROLE_${role.toUpperCase()}`;

      return matchesSearch && matchesRole;
    });
  });

  // Modal State for Delete
  userToDelete = signal<UserResponse | null>(null);
  isDeleting = signal<boolean>(false);

  // Modal State for Create User
  isCreateModalOpen = signal<boolean>(false);
  isCreating = signal<boolean>(false);
  createForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    isAdmin: [false]
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.userService.findAll().subscribe({
      next: (data) => {
        this.users.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Falha ao carregar lista de usuários');
        this.isLoading.set(false);
        this.notificationService.error('Não foi possível listar os usuários.');
      }
    });
  }

  // Delete Handlers
  openDeleteConfirm(user: UserResponse): void {
    this.userToDelete.set(user);
  }

  cancelDelete(): void {
    this.userToDelete.set(null);
  }

  confirmDelete(): void {
    const user = this.userToDelete();
    if (!user) return;

    this.isDeleting.set(true);
    this.userService.delete(user.id).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.notificationService.success(`Usuário "${user.name}" excluído com sucesso!`);
        this.users.update(list => list.filter(u => u.id !== user.id));
        this.userToDelete.set(null);
      },
      error: (err) => {
        this.isDeleting.set(false);
        const msg = err.error?.message || 'Erro ao excluir usuário.';
        this.notificationService.error(msg);
      }
    });
  }

  // Create Handlers
  openCreateModal(): void {
    this.createForm.reset({ isAdmin: false });
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.isCreating.set(true);
    const { name, email, password, isAdmin } = this.createForm.value;
    this.authService.register({ name, email, password, isAdmin: Boolean(isAdmin) }).subscribe({
      next: (created) => {
        this.isCreating.set(false);
        this.isCreateModalOpen.set(false);
        this.notificationService.success(`Usuário "${created.name}" cadastrado com sucesso!`);
        this.loadUsers();
      },
      error: (err) => {
        this.isCreating.set(false);
        const msg = err.error?.message || 'Erro ao registrar usuário. Verifique se o e-mail já existe ou se você possui permissão.';
        this.notificationService.error(msg);
      }
    });
  }
}

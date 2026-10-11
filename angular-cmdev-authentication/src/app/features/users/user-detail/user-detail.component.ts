import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-user-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './user-detail.component.html',
  styleUrl: './user-detail.component.scss'
})
export class UserDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly notificationService = inject(NotificationService);

  user = signal<UserResponse | null>(null);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);

  // Delete modal state
  showDeleteModal = signal<boolean>(false);
  isDeleting = signal<boolean>(false);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.loadUser(Number(idParam));
    } else {
      this.errorMessage.set('ID de usuário não fornecido');
      this.isLoading.set(false);
    }
  }

  loadUser(id: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.userService.findById(id).subscribe({
      next: (userData) => {
        this.user.set(userData);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 404) {
          this.errorMessage.set('Usuário não encontrado.');
        } else {
          this.errorMessage.set(err.error?.message || 'Erro ao carregar dados do usuário.');
        }
        this.notificationService.error(this.errorMessage()!);
      }
    });
  }

  confirmDelete(): void {
    const currentUser = this.user();
    if (!currentUser) return;

    this.isDeleting.set(true);
    this.userService.delete(currentUser.id).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.notificationService.success(`Usuário #${currentUser.id} excluído com sucesso!`);
        this.router.navigate(['/users']);
      },
      error: (err) => {
        this.isDeleting.set(false);
        this.notificationService.error(err.error?.message || 'Falha ao excluir usuário.');
      }
    });
  }
}

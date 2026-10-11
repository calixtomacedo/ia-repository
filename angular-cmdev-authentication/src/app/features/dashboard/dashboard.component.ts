import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { ConfigService } from '../../core/services/config.service';
import { UserResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  readonly authService = inject(AuthService);
  readonly userService = inject(UserService);
  readonly configService = inject(ConfigService);

  userList = signal<UserResponse[]>([]);
  isLoadingUsers = signal<boolean>(true);
  usersError = signal<string | null>(null);

  totalUsers = signal<number>(0);
  adminCount = signal<number>(0);
  standardCount = signal<number>(0);

  ngOnInit(): void {
    this.loadUsers();
    this.configService.checkHealth().subscribe();
  }

  loadUsers(): void {
    this.isLoadingUsers.set(true);
    this.usersError.set(null);

    this.userService.findAll().subscribe({
      next: (users) => {
        this.userList.set(users);
        this.totalUsers.set(users.length);
        this.adminCount.set(users.filter(u => u.role === 'ADMIN' || u.role === 'ROLE_ADMIN').length);
        this.standardCount.set(users.filter(u => u.role !== 'ADMIN' && u.role !== 'ROLE_ADMIN').length);
        this.isLoadingUsers.set(false);
      },
      error: (err) => {
        this.usersError.set(err.error?.message || 'Falha ao carregar lista de usuários');
        this.isLoadingUsers.set(false);
      }
    });
  }
}

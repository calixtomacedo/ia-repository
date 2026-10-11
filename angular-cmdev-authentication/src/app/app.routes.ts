import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard'
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    title: 'Login | Atom Auth'
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    title: 'Cadastro | Atom Auth'
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    title: 'Dashboard | Atom Auth'
  },
  {
    path: 'users',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/users/user-list/user-list.component').then(m => m.UserListComponent),
        title: 'Usuários | Atom Auth'
      },
      {
        path: ':id',
        loadComponent: () => import('./features/users/user-detail/user-detail.component').then(m => m.UserDetailComponent),
        title: 'Detalhes do Usuário | Atom Auth'
      },
      {
        path: ':id/edit',
        loadComponent: () => import('./features/users/user-edit/user-edit.component').then(m => m.UserEditComponent),
        title: 'Editar Usuário | Atom Auth'
      }
    ]
  },
  {
    path: 'tokens',
    canActivate: [authGuard],
    loadComponent: () => import('./features/tokens/tokens.component').then(m => m.TokensComponent),
    title: 'Tokens & Sessão JWT | Atom Auth'
  },
  {
    path: 'api-explorer',
    loadComponent: () => import('./features/api-explorer/api-explorer.component').then(m => m.ApiExplorerComponent),
    title: 'API Explorer | Atom Auth'
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];

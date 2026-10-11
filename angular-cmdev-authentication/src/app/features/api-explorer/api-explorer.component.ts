import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ConfigService } from '../../core/services/config.service';
import { AuthService } from '../../core/services/auth.service';

export interface EndpointDef {
  id: string;
  category: 'auth' | 'users';
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  path: string;
  summary: string;
  authRequired: boolean;
  defaultBody?: string;
  hasPathParam?: boolean;
  pathParamName?: string;
  defaultPathParam?: string;
}

@Component({
  selector: 'app-api-explorer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './api-explorer.component.html',
  styleUrl: './api-explorer.component.scss'
})
export class ApiExplorerComponent {
  private readonly http = inject(HttpClient);
  readonly configService = inject(ConfigService);
  readonly authService = inject(AuthService);

  readonly endpoints: EndpointDef[] = [
    {
      id: 'login',
      category: 'auth',
      method: 'POST',
      path: '/auth/login',
      summary: 'Realizar login (autenticação pública)',
      authRequired: false,
      defaultBody: JSON.stringify({ email: 'admin@empresa.com', password: 'Admin@123' }, null, 2)
    },
    {
      id: 'register',
      category: 'auth',
      method: 'POST',
      path: '/auth/register',
      summary: 'Cadastrar novo usuário (permite flag opcional isAdmin)',
      authRequired: true,
      defaultBody: JSON.stringify({ name: 'Maria Souza', email: 'maria.souza@empresa.com', password: 'Senha@123', isAdmin: false }, null, 2)
    },
    {
      id: 'refresh-token',
      category: 'auth',
      method: 'POST',
      path: '/auth/refresh-token',
      summary: 'Renovar token JWT com refresh token',
      authRequired: false,
      defaultBody: JSON.stringify({ refreshToken: this.authService.refreshToken() || 'coloque-o-refresh-token-aqui' }, null, 2)
    },
    {
      id: 'logout',
      category: 'auth',
      method: 'POST',
      path: '/auth/logout',
      summary: 'Realizar logout e revogar token',
      authRequired: true
    },
    {
      id: 'list-users',
      category: 'users',
      method: 'GET',
      path: '/users',
      summary: 'Listar todos os usuários',
      authRequired: true
    },
    {
      id: 'get-user',
      category: 'users',
      method: 'GET',
      path: '/users/{id}',
      summary: 'Buscar usuário por ID',
      authRequired: true,
      hasPathParam: true,
      pathParamName: 'id',
      defaultPathParam: '1'
    },
    {
      id: 'update-user',
      category: 'users',
      method: 'PUT',
      path: '/users/{id}',
      summary: 'Atualizar usuário por ID',
      authRequired: true,
      hasPathParam: true,
      pathParamName: 'id',
      defaultPathParam: '1',
      defaultBody: JSON.stringify({ name: 'Administrador Atualizado', email: 'admin@empresa.com' }, null, 2)
    },
    {
      id: 'delete-user',
      category: 'users',
      method: 'DELETE',
      path: '/users/{id}',
      summary: 'Remover usuário por ID',
      authRequired: true,
      hasPathParam: true,
      pathParamName: 'id',
      defaultPathParam: '2'
    }
  ];

  selectedEndpoint = signal<EndpointDef>(this.endpoints[0]);
  requestBody = signal<string>(this.endpoints[0].defaultBody || '');
  pathParamValue = signal<string>(this.endpoints[0].defaultPathParam || '');

  readonly fullRequestUrl = signal<string>('');

  constructor() {
    this.updateFullUrl();
  }

  private updateFullUrl(): void {
    const ep = this.selectedEndpoint();
    let path = ep.path;
    if (ep.hasPathParam && ep.pathParamName) {
      path = path.replace(`{${ep.pathParamName}}`, this.pathParamValue() || '1');
    }
    this.fullRequestUrl.set(`${this.configService.apiUrl()}${path}`);
  }

  // Response state
  isExecuting = signal<boolean>(false);
  responseStatus = signal<number | null>(null);
  responseTimeMs = signal<number | null>(null);
  responseHeaders = signal<Record<string, string>>({});
  responseBody = signal<string | null>(null);

  selectEndpoint(ep: EndpointDef): void {
    this.selectedEndpoint.set(ep);
    this.requestBody.set(ep.defaultBody || '');
    this.pathParamValue.set(ep.defaultPathParam || '');
    this.responseStatus.set(null);
    this.responseBody.set(null);
    this.updateFullUrl();
  }

  onPathParamChange(val: string): void {
    this.pathParamValue.set(val);
    this.updateFullUrl();
  }

  executeRequest(): void {
    const ep = this.selectedEndpoint();
    let url = `${this.configService.apiUrl()}${ep.path}`;

    if (ep.hasPathParam && ep.pathParamName) {
      url = url.replace(`{${ep.pathParamName}}`, encodeURIComponent(this.pathParamValue() || '1'));
    }

    this.isExecuting.set(true);
    this.responseStatus.set(null);
    this.responseBody.set(null);

    const startTime = performance.now();

    let parsedBody = undefined;
    if (ep.defaultBody !== undefined && this.requestBody()) {
      try {
        parsedBody = JSON.parse(this.requestBody());
      } catch {
        parsedBody = this.requestBody();
      }
    }

    let requestObservable;
    switch (ep.method) {
      case 'GET':
        requestObservable = this.http.get(url, { observe: 'response', responseType: 'text' });
        break;
      case 'POST':
        requestObservable = this.http.post(url, parsedBody, { observe: 'response', responseType: 'text' });
        break;
      case 'PUT':
        requestObservable = this.http.put(url, parsedBody, { observe: 'response', responseType: 'text' });
        break;
      case 'DELETE':
        requestObservable = this.http.delete(url, { observe: 'response', responseType: 'text' });
        break;
    }

    requestObservable.subscribe({
      next: (response: HttpResponse<string>) => {
        const endTime = performance.now();
        this.isExecuting.set(false);
        this.responseStatus.set(response.status);
        this.responseTimeMs.set(Math.round(endTime - startTime));

        // Format body if JSON
        let text = response.body || '';
        try {
          text = JSON.stringify(JSON.parse(text), null, 2);
        } catch {
          // not JSON, keep as text
        }
        this.responseBody.set(text || '(Corpo da resposta vazio / No Content)');
      },
      error: (err) => {
        const endTime = performance.now();
        this.isExecuting.set(false);
        this.responseStatus.set(err.status || 0);
        this.responseTimeMs.set(Math.round(endTime - startTime));

        let text = typeof err.error === 'string' ? err.error : JSON.stringify(err.error, null, 2);
        this.responseBody.set(text || err.message || 'Erro de rede');
      }
    });
  }
}

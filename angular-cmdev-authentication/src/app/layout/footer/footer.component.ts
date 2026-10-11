import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfigService } from '../../core/services/config.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss'
})
export class FooterComponent {
  readonly configService = inject(ConfigService);
  readonly notificationService = inject(NotificationService);

  customUrl: string = this.configService.apiUrl();
  isEditingUrl: boolean = false;

  toggleEditUrl(): void {
    this.customUrl = this.configService.apiUrl();
    this.isEditingUrl = !this.isEditingUrl;
  }

  saveUrl(): void {
    if (this.customUrl && this.customUrl.trim()) {
      this.configService.setApiUrl(this.customUrl);
      this.isEditingUrl = false;
      this.notificationService.success('URL da API atualizada com sucesso!');
    }
  }

  resetUrl(): void {
    this.configService.resetApiUrl();
    this.customUrl = this.configService.apiUrl();
    this.isEditingUrl = false;
    this.notificationService.info('URL restaurada para o padrão.');
  }

  testHealth(): void {
    this.configService.checkHealth().subscribe(healthy => {
      if (healthy) {
        this.notificationService.success('API está online e respondendo perfeitamente!', 'Conexão OK');
      } else {
        this.notificationService.error('Não foi possível conectar à API. Verifique se o backend está rodando.', 'Falha na Conexão');
      }
    });
  }
}

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegisterComponent } from './register.component';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { of } from 'rxjs';
import { UserResponse } from '../../../core/models/user.model';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['register', 'isAuthenticated', 'isAdmin']);
    authServiceSpy.isAuthenticated.and.returnValue(false);
    authServiceSpy.isAdmin.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        {
          provide: NotificationService,
          useValue: {
            success: jasmine.createSpy('success'),
            error: jasmine.createSpy('error'),
            info: jasmine.createSpy('info')
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component with isAdmin control initialized to false', () => {
    expect(component).toBeTruthy();
    expect(component.registerForm.get('isAdmin')?.value).toBeFalse();
  });

  it('should include isAdmin in payload when submitting form with true', () => {
    const mockUserResponse: UserResponse = {
      id: 1,
      name: 'Admin User',
      email: 'admin@empresa.com',
      role: 'ROLE_ADMIN',
      createdAt: '2026-10-11T12:00:00Z'
    };
    authServiceSpy.register.and.returnValue(of(mockUserResponse));

    component.registerForm.setValue({
      name: 'Admin User',
      email: 'admin@empresa.com',
      password: 'Password@123',
      confirmPassword: 'Password@123',
      isAdmin: true
    });

    component.onSubmit();

    expect(authServiceSpy.register).toHaveBeenCalledWith({
      name: 'Admin User',
      email: 'admin@empresa.com',
      password: 'Password@123',
      isAdmin: true
    });
    expect(component.registrationSuccess).toBeTrue();
    expect(component.registeredRole).toBe('ROLE_ADMIN');
  });

  it('should include isAdmin as false when switch is left unchecked', () => {
    const mockUserResponse: UserResponse = {
      id: 2,
      name: 'Regular User',
      email: 'user@empresa.com',
      role: 'ROLE_USER',
      createdAt: '2026-10-11T12:00:00Z'
    };
    authServiceSpy.register.and.returnValue(of(mockUserResponse));

    component.registerForm.setValue({
      name: 'Regular User',
      email: 'user@empresa.com',
      password: 'Password@123',
      confirmPassword: 'Password@123',
      isAdmin: false
    });

    component.onSubmit();

    expect(authServiceSpy.register).toHaveBeenCalledWith({
      name: 'Regular User',
      email: 'user@empresa.com',
      password: 'Password@123',
      isAdmin: false
    });
    expect(component.registrationSuccess).toBeTrue();
    expect(component.registeredRole).toBe('ROLE_USER');
  });
});

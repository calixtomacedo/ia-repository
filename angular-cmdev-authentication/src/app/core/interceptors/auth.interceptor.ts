import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';
import { Router } from '@angular/router';

let isRefreshing = false;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.accessToken();

  let clonedReq = req;
  // Attach token if available and not already set
  if (token && !req.headers.has('Authorization')) {
    clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Avoid infinite loop if 401 is from login or refresh-token
      const isAuthEndpoint = req.url.includes('/auth/login') || req.url.includes('/auth/refresh-token');

      if (error.status === 401 && !isAuthEndpoint && authService.refreshToken() && !isRefreshing) {
        isRefreshing = true;
        return authService.refreshCurrentToken().pipe(
          switchMap(authResponse => {
            isRefreshing = false;
            // Retry original request with newly issued access token
            const retriedReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${authResponse.accessToken}`
              }
            });
            return next(retriedReq);
          }),
          catchError(refreshErr => {
            isRefreshing = false;
            authService.clearSession();
            router.navigate(['/login']);
            return throwError(() => refreshErr);
          })
        );
      }

      if (error.status === 401 && !isAuthEndpoint) {
        authService.clearSession();
        router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};

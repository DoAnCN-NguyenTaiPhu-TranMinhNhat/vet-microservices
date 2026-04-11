'use strict';

/**
 * Global HTTP errors handler.
 * Version: 2025-02-08-16-45 - Fixed null response.data and multiple error formats
 */
angular.module('infrastructure')
    .factory('HttpErrorHandlingInterceptor', function () {
        return {
            responseError: function (response) {
                if (response.status === 401) {
                    try {
                        localStorage.removeItem('vet_clinic_jwt');
                        localStorage.removeItem('vet_clinic_user');
                    } catch (e) { /* ignore */ }
                    var hash = window.location.hash || '';
                    if (hash.indexOf('login') < 0 && hash.indexOf('register') < 0) {
                        window.location.href = window.location.pathname + window.location.search + '#!/login';
                    }
                    return Promise.reject(response);
                }

                // Visits page handles feedback errors (banner); skip duplicate global alert.
                if (response.config && response.config.skipGlobalErrorAlert) {
                    return Promise.reject(response);
                }

                console.log('HTTP Error Interceptor - Response:', response);

                var error = response.data || {};
                console.log('HTTP Error Interceptor - Error object:', error);
                
                // Handle null/undefined error.data
                if (!error || typeof error !== 'object') {
                    console.log('HTTP Error Interceptor - Using fallback error handling');
                    var errorMessage = "Server Error (" + response.status + ")";
                    alert(errorMessage);
                    return Promise.reject(response);
                }
                
                // Spring validation format
                if (Array.isArray(error.errors)) {
                    console.log('HTTP Error Interceptor - Using Spring validation format');
                    var errorMessage = (error.error || "Validation Error") + "\n" +
                        error.errors.map(function (e) {
                            return e.field + ": " + e.defaultMessage;
                        }).join("\n");
                    alert(errorMessage);
                }
                // FastAPI validation format
                else if (Array.isArray(error.detail)) {
                    console.log('HTTP Error Interceptor - Using FastAPI validation format');
                    var errorMessage = "Validation Error\n" +
                        error.detail.map(function (e) {
                            return e.loc.join(".") + ": " + e.msg;
                        }).join("\n");
                    alert(errorMessage);
                }
                // Default Spring error or other formats
                else {
                    console.log('HTTP Error Interceptor - Using default error handling');
                    var errorMessage = error.message || error.error || error.detail || "Server Error";
                    
                    // Add status code for debugging
                    if (response.status) {
                        errorMessage += " (" + response.status + ")";
                    }
                    
                    // Add path if available
                    if (error.path) {
                        errorMessage += " - Path: " + error.path;
                    }
                    
                    alert(errorMessage);
                }
                
                // Important: reject the promise to propagate the error
                return Promise.reject(response);
            }
        }
    });

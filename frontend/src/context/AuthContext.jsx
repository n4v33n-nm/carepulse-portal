import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem('carepulse_user');
    const token = localStorage.getItem('carepulse_token');
    if (storedUser && token) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        localStorage.removeItem('carepulse_user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (credentials) => {
    const response = await authService.login(credentials);
    const data = response.data;
    localStorage.setItem('carepulse_token', data.token);
    localStorage.setItem('carepulse_user', JSON.stringify(data));
    setUser(data);
    return data;
  };

  const registerPatient = async (formData) => {
    const response = await authService.registerPatient(formData);
    const data = response.data;
    localStorage.setItem('carepulse_token', data.token);
    localStorage.setItem('carepulse_user', JSON.stringify(data));
    setUser(data);
    return data;
  };

  const registerDoctor = async (formData) => {
    const response = await authService.registerDoctor(formData);
    const data = response.data;
    localStorage.setItem('carepulse_token', data.token);
    localStorage.setItem('carepulse_user', JSON.stringify(data));
    setUser(data);
    return data;
  };

  const updateEmpathyPreference = async (preference) => {
    await authService.updateCommunicationPreference(preference);
    const updated = { ...user, communicationPreference: preference };
    localStorage.setItem('carepulse_user', JSON.stringify(updated));
    setUser(updated);
  };

  const logout = () => {
    try {
      authService.logout();
    } catch (e) {
      // ignore
    }
    localStorage.removeItem('carepulse_token');
    localStorage.removeItem('carepulse_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        registerPatient,
        registerDoctor,
        updateEmpathyPreference,
        logout,
        isAuthenticated: !!user,
        role: user?.role,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

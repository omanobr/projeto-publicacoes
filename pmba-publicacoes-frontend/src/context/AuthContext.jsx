import { createContext, useState, useContext } from 'react';

// 1. Cria o Contexto
const AuthContext = createContext();

// 2. Cria o Provedor (um componente que vai "abraçar" nossa aplicação)
export function AuthProvider({ children }) {
  const [isAdmin, setIsAdmin] = useState(false);

  // Função para simular login/logout
  const toggleAdmin = () => setIsAdmin(!isAdmin);

  return (
    <AuthContext.Provider value={{ isAdmin, toggleAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

// 3. Cria um "hook" customizado para facilitar o uso
export function useAuth() {
  return useContext(AuthContext);
}
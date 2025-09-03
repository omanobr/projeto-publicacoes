import React from 'react';
import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';

import App from './App.jsx';
import HomePage from './pages/HomePage.jsx';
import PublicacaoPage from './pages/PublicacaoPage.jsx';
import AdminPage from './pages/AdminPage.jsx';
import EditPage from './pages/EditPage.jsx';
import BuscaAvancadaPage from './pages/BuscaAvancadaPage.jsx';
import { AuthProvider } from './context/AuthContext';
import './index.css';

const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: "/publicacao/:id",
        element: <PublicacaoPage />,
      },
      { // <-- 2. ADICIONE ESTA NOVA ROTA
        path: "/admin",
        element: <AdminPage />,
      },
      { // <-- 2. ADICIONE ESTA NOVA ROTA DINÂMICA
        path: "/admin/editar/:id",
        element: <EditPage />,
      },
      { // <-- ADICIONE ESTA NOVA ROTA
        path: "/busca-avancada",
        element: <BuscaAvancadaPage />,
      }
    ],
  },
]);

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    {/* VVV--- Envolva o RouterProvider com o AuthProvider ---VVV */}
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
    {/* ^^^----------------------------------------------------^^^ */}
  </React.StrictMode>,
);
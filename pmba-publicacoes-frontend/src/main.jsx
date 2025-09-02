import React from 'react';
import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';

import App from './App.jsx';
import HomePage from './pages/HomePage.jsx';
import PublicacaoPage from './pages/PublicacaoPage.jsx';
import AdminPage from './pages/AdminPage.jsx';
import EditPage from './pages/EditPage.jsx';

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
      }
    ],
  },
]);

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
);
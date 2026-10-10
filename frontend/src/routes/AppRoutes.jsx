import { Route, Routes } from 'react-router-dom'

import AdminLayout from '../layouts/AdminLayout.jsx'
import MainLayout from '../layouts/MainLayout.jsx'
import AdminClaims from '../pages/AdminClaims.jsx'
import AdminDashboard from '../pages/AdminDashboard.jsx'
import AdminItems from '../pages/AdminItems.jsx'
import AdminUsers from '../pages/AdminUsers.jsx'
import EditItem from '../pages/EditItem.jsx'
import Home from '../pages/Home.jsx'
import ItemDetail from '../pages/ItemDetail.jsx'
import Login from '../pages/Login.jsx'
import MyClaims from '../pages/MyClaims.jsx'
import MyItems from '../pages/MyItems.jsx'
import NotFound from '../pages/NotFound.jsx'
import Register from '../pages/Register.jsx'
import ReportItem from '../pages/ReportItem.jsx'
import Profile from '../pages/Profile.jsx'
import ProtectedRoute from './ProtectedRoute.jsx'

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        {/* Public */}
        <Route index element={<Home />} />
        <Route path="items/:id" element={<ItemDetail />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />

        {/* Any logged-in user */}
        <Route element={<ProtectedRoute />}>
          <Route path="profile" element={<Profile />} />
        </Route>

        {/* Students only (the backend does not let admins report or claim) */}
        <Route element={<ProtectedRoute role="STUDENT" />}>
          <Route path="report" element={<ReportItem />} />
          <Route path="items/:id/edit" element={<EditItem />} />
          <Route path="my-items" element={<MyItems />} />
          <Route path="my-claims" element={<MyClaims />} />
        </Route>

        {/* Admins only */}
        <Route element={<ProtectedRoute role="ADMIN" />}>
          <Route path="admin" element={<AdminLayout />}>
            <Route index element={<AdminDashboard />} />
            <Route path="claims" element={<AdminClaims />} />
            <Route path="items" element={<AdminItems />} />
            <Route path="users" element={<AdminUsers />} />
          </Route>
        </Route>

        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}

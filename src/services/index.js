import api from './api'

export const authService = {
  register: (name, email, password) => api.post('/auth/register', { name, email, password }),
  login:    (email, password)        => api.post('/auth/login',    { email, password }),
}

export const projectService = {
  getAll:       (page = 0, size = 20) => api.get('/projects', { params: { page, size } }),
  getById:      (id)                  => api.get(`/projects/${id}`),
  create:       (data)                => api.post('/projects', data),
  delete:       (id)                  => api.delete(`/projects/${id}`),
  addMember:    (projectId, userId)   => api.post(`/projects/${projectId}/members`, { userId }),
  removeMember: (projectId, userId)   => api.delete(`/projects/${projectId}/members/${userId}`),
}

export const sprintService = {
  getAll:    (projectId)          => api.get(`/projects/${projectId}/sprints`),
  create:    (projectId, data)    => api.post(`/projects/${projectId}/sprints`, data),
  start:     (sprintId)           => api.patch(`/sprints/${sprintId}/start`),
  complete:  (sprintId)           => api.patch(`/sprints/${sprintId}/complete`),
  addIssue:  (sprintId, issueId)  => api.post(`/sprints/${sprintId}/issues`, { issueId }),
  getBoard:  (projectId)          => api.get(`/projects/${projectId}/board`),
}

export const issueService = {
  getAll:       (projectId, filters = {}, page = 0, size = 50) =>
                  api.get(`/projects/${projectId}/issues`, { params: { page, size, ...filters } }),
  getById:      (issueId)         => api.get(`/issues/${issueId}`),
  create:       (projectId, data) => api.post(`/projects/${projectId}/issues`, data),
  update:       (issueId, data)   => api.put(`/issues/${issueId}`, data),
  updateStatus: (issueId, status) => api.patch(`/issues/${issueId}/status`, { status }),
  delete:       (issueId)         => api.delete(`/issues/${issueId}`),
}

export const commentService = {
  getAll: (issueId)         => api.get(`/issues/${issueId}/comments`),
  add:    (issueId, body)   => api.post(`/issues/${issueId}/comments`, { body }),
  delete: (commentId)       => api.delete(`/comments/${commentId}`),
}

export const notificationService = {
  getUnread:   () => api.get('/notifications'),
  getCount:    () => api.get('/notifications/count'),
  markAllRead: () => api.patch('/notifications/read-all'),
}

// Returns all users — used for member/assignee dropdowns
export const userService = {
  getAll:     (page = 0, size = 100) => api.get('/users', { params: { page, size } }),
  getById:    (id)                   => api.get(`/users/${id}`),
  updateRole: (id, role)             => api.patch(`/users/${id}/role`, { role }),
}
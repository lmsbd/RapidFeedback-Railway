import axios from 'axios';
import request from '@/utils/request';
import userStore from '@/stores/userStore';

function extractData(res) {
  if (res?.data !== undefined) return res.data;
  if (res?.response !== undefined) return res.response;
  return res;
}

export async function getFinalMarkList(projectId) {
  try {
    const res = await request.get('/finalMark/list', {
      params: { projectId: Number(projectId) },
    });
    return extractData(res);
  } catch (error) {
    console.error('getFinalMarkList error:', error);
    return null;
  }
}

export async function saveFinalMark({ projectId, studentId, groupId, finalScore }) {
  return request.post('/finalMark/save', {
    projectId: Number(projectId),
    studentId: studentId != null ? Number(studentId) : null,
    groupId: groupId != null ? Number(groupId) : null,
    finalScore,
  });
}

export async function lockFinalMark({ projectId, studentId, groupId, isLocked }) {
  return request.post('/finalMark/lock', {
    projectId: Number(projectId),
    studentId: studentId != null ? Number(studentId) : null,
    groupId: groupId != null ? Number(groupId) : null,
    isLocked,
  });
}

export async function publishReport(projectId) {
  return request.post('/projects/sendReport', { projectId: Number(projectId) });
}

export async function exportRawFinalMarkExcel(projectId, projectType) {
  const exportType = String(projectType || '').toLowerCase() === 'group' ? 'group' : 'individual';
  return request.get(`/export/${exportType}/${Number(projectId)}`, {
    responseType: 'blob',
  });
}

export async function importMarkExcel(projectId, projectType, file) {
  const importType = String(projectType || '').toLowerCase() === 'group' ? 'group' : 'individual';
  const formData = new FormData();
  formData.append('file', file);
  const headers = {};
  if (userStore.token) {
    headers.Authorization = `Bearer ${userStore.token}`;
  }
  const res = await axios.post(`/api/import/${importType}/${Number(projectId)}`, formData, {
    headers,
    timeout: 60000,
  });
  return res.data;
}

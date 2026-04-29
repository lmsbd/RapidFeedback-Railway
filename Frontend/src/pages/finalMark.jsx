import { useEffect, useState, useCallback, useMemo } from 'react';
import { useParams, useLocation } from 'umi';
import {
  Button,
  Card,
  Table,
  Typography,
  InputNumber,
  Tag,
  message,
  Tooltip,
  Popconfirm,
} from 'antd';
import {
  LockOutlined,
  UnlockOutlined,
  SaveOutlined,
  CheckCircleOutlined,
  SendOutlined,
} from '@ant-design/icons';
import BackButton from '../components/BackButton/BackButton';
import { getFinalMarkList, saveFinalMark, lockFinalMark, publishReport } from '../apis/finalMark';
import { getProjectDetail } from '../apis/getProjectDetail';
import styles from './finalMark.module.less';

const { Title, Text } = Typography;

function getStudentName(record) {
  return `${record?.firstName ?? ''} ${record?.surname ?? ''}`.trim();
}

function toScore(value) {
  if (value === null || value === undefined || value === '') return null;
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
}

function formatScore(value) {
  const num = toScore(value);
  return num != null ? num.toFixed(2) : '--';
}

function getAverageScore(record) {
  const scores = (record?.markerScores || [])
    .map((s) => toScore(s.score))
    .filter((v) => v != null);
  if (!scores.length) return null;
  return scores.reduce((a, b) => a + b, 0) / scores.length;
}

function getProjectType(rawType) {
  const type = String(rawType || '').toLowerCase();
  if (type.includes('group') || type.includes('team')) return 'group';
  return 'individual';
}

function clampPaginationState(prev, totalItems) {
  const safePageSize =
    Number.isFinite(prev?.pageSize) && prev.pageSize > 0 ? prev.pageSize : 10;
  const totalPages = Math.max(1, Math.ceil((totalItems || 0) / safePageSize));
  const safeCurrent =
    Number.isFinite(prev?.current) && prev.current > 0 ? prev.current : 1;
  if (safeCurrent <= totalPages && safePageSize === prev?.pageSize) return prev;
  return { current: Math.min(safeCurrent, totalPages), pageSize: safePageSize };
}

export default function FinalMark() {
  const { projectId } = useParams();
  const location = useLocation();
  const routeProjectName = location.state?.projectName || '';
  const routeProjectType = location.state?.projectType || '';
  const routeSubjectName = location.state?.subjectName || '';

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState({});
  const [locking, setLocking] = useState({});
  const [items, setItems] = useState([]);
  const [projectType, setProjectType] = useState('individual');
  const [projectName, setProjectName] = useState(routeProjectName || 'Project');
  const [subjectName, setSubjectName] = useState(routeSubjectName || '');
  const [editedScores, setEditedScores] = useState({});
  const [publishing, setPublishing] = useState(false);
  const [savingAll, setSavingAll] = useState(false);
  const [maxFinalMark, setMaxFinalMark] = useState(100);
  const [tablePagination, setTablePagination] = useState({
    current: 1,
    pageSize: 10,
  });

  const isGroupProject = projectType === 'group';
  const allConfirmed = items.length > 0 && items.every((item) => !!item.isLocked);

  const hasUnsavedEdits = useMemo(
    () => Object.keys(editedScores).some((k) => editedScores[k] !== undefined),
    [editedScores]
  );

  const allMarkerNames = useMemo(() => {
    const nameSet = new Map();
    items.forEach((item) => {
      (item.markerScores || []).forEach((ms) => {
        if (!nameSet.has(ms.markerId)) {
          nameSet.set(ms.markerId, ms.markerName || `Marker ${ms.markerId}`);
        }
      });
    });
    return Array.from(nameSet.entries()).map(([id, name]) => ({ id, name }));
  }, [items]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [detail, listData] = await Promise.all([
        getProjectDetail(projectId),
        getFinalMarkList(projectId),
      ]);

      if (detail) {
        const type = getProjectType(detail.projectType || routeProjectType);
        setProjectType(type);
        setProjectName(routeProjectName || detail.projectName || 'Project');
      }

      if (listData) {
        const responseData = listData.response || listData;
        const dataItems = Array.isArray(responseData.items || responseData)
          ? responseData.items || responseData
          : [];
        setItems(dataItems);
        const parsedMaxFinalMark = Number(responseData?.weightedMaxScore);
        setMaxFinalMark(
          Number.isFinite(parsedMaxFinalMark) && parsedMaxFinalMark > 0
            ? parsedMaxFinalMark
            : 100
        );
        if (responseData.projectType) {
          setProjectType(getProjectType(responseData.projectType));
        }
        if (responseData.projectName) {
          setProjectName(responseData.projectName);
        }
      }
    } catch (e) {
      console.error(e);
      message.error('Failed to load final mark data');
    } finally {
      setLoading(false);
    }
  }, [projectId, routeProjectName, routeProjectType]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    setTablePagination((prev) => ({ ...prev, current: 1 }));
  }, [projectType]);

  useEffect(() => {
    setTablePagination((prev) => clampPaginationState(prev, items.length));
  }, [items.length]);

  const pagination = useMemo(
    () => ({
      current: tablePagination.current,
      pageSize: tablePagination.pageSize,
      showSizeChanger: true,
      pageSizeOptions: ['10', '20', '50', '100'],
      onChange: (nextPage, nextPageSize) => {
        setTablePagination((prev) => ({
          current: nextPageSize !== prev.pageSize ? 1 : nextPage,
          pageSize: nextPageSize,
        }));
      },
    }),
    [tablePagination.current, tablePagination.pageSize]
  );

  const getRowKey = useCallback(
    (record) => {
      return isGroupProject
        ? `group-${record.groupId ?? record.id}-${record.studentId ?? record.id ?? 'na'}`
        : `student-${record.studentId ?? record.id}`;
    },
    [isGroupProject]
  );

  const getEditKey = useCallback(
    (record) => {
      return `s-${record.studentId ?? record.id}`;
    },
    []
  );

  const handleScoreChange = useCallback(
    (record, value) => {
      const key = getEditKey(record);
      if (value == null) {
        setEditedScores((prev) => ({ ...prev, [key]: value }));
        return;
      }
      const clampedValue = Math.min(Math.max(Number(value), 0), maxFinalMark);
      setEditedScores((prev) => ({ ...prev, [key]: clampedValue }));
    },
    [getEditKey, maxFinalMark]
  );

  const getCurrentScore = useCallback(
    (record) => {
      const key = getEditKey(record);
      if (editedScores[key] !== undefined) return editedScores[key];
      if (record.finalScore != null) return Number(record.finalScore);
      return getAverageScore(record);
    },
    [editedScores, getEditKey]
  );

  const handleSave = useCallback(
    async (record) => {
      const key = getEditKey(record);
      const score = getCurrentScore(record);
      const groupId = isGroupProject ? Number(record.groupId ?? record.id) : null;

      if (score == null) {
        message.warning('Please enter a final mark before saving');
        return false;
      }
      if (score < 0 || score > maxFinalMark) {
        message.warning(`Final mark must be between 0 and ${maxFinalMark.toFixed(2)}`);
        return false;
      }

      setSaving((prev) => ({ ...prev, [key]: true }));
      try {
        await saveFinalMark({
          projectId: Number(projectId),
          studentId: record.studentId ?? record.id,
          groupId,
          finalScore: score,
        });
        message.success('Final mark saved');
        setEditedScores((prev) => {
          const next = { ...prev };
          delete next[key];
          return next;
        });
        await fetchData();
        return true;
      } catch (e) {
        console.error(e);
        message.error(e?.response?.data?.msg || 'Failed to save final mark');
        return false;
      } finally {
        setSaving((prev) => ({ ...prev, [key]: false }));
      }
    },
    [projectId, isGroupProject, getEditKey, getCurrentScore, fetchData, maxFinalMark]
  );

  const handleConfirmWithSave = useCallback(
    async (record) => {
      const saved = await handleSave(record);
      if (!saved) return;
      await handleLock(record, true);
    },
    [handleSave, handleLock]
  );

  const handleLock = useCallback(
    async (record, lock) => {
      const key = getEditKey(record);
      setLocking((prev) => ({ ...prev, [key]: true }));
      try {
        const groupId = isGroupProject ? Number(record.groupId ?? record.id) : null;
        await lockFinalMark({
          projectId: Number(projectId),
          studentId: record.studentId ?? record.id,
          groupId,
          isLocked: lock ? 1 : 0,
        });
        message.success(lock ? 'Final mark confirmed' : 'Final mark unconfirmed');
        await fetchData();
      } catch (e) {
        console.error(e);
        message.error(e?.response?.data?.msg || 'Failed to update confirmation status');
      } finally {
        setLocking((prev) => ({ ...prev, [key]: false }));
      }
    },
    [projectId, isGroupProject, getEditKey, fetchData]
  );

  const handleSaveAll = useCallback(
    async (records = items, options = {}) => {
      const { showSuccess = true, refreshAfterSave = true } = options;
      const saveTargets = records.filter((item) => {
        if (item.isLocked) return false;
        return getCurrentScore(item) != null;
      });

      if (!saveTargets.length) {
        message.info('No unlocked items with final marks to save');
        return false;
      }

      setSavingAll(true);
      try {
        let successCount = 0;
        for (const record of saveTargets) {
          const score = getCurrentScore(record);
          if (score == null) continue;
          if (score < 0 || score > maxFinalMark) {
            message.warning(`Final mark must be between 0 and ${maxFinalMark.toFixed(2)}`);
            return false;
          }

          const groupId = isGroupProject ? Number(record.groupId ?? record.id) : null;
          await saveFinalMark({
            projectId: Number(projectId),
            studentId: record.studentId ?? record.id,
            groupId,
            finalScore: score,
          });
          successCount++;
        }

        setEditedScores({});
        if (showSuccess) {
          message.success(`Saved ${successCount} final marks`);
        }
        if (refreshAfterSave) {
          await fetchData();
        }
        return true;
      } catch (e) {
        console.error(e);
        message.error('Failed to save all final marks, some may have been saved');
        if (refreshAfterSave) {
          await fetchData();
        }
        return false;
      } finally {
        setSavingAll(false);
      }
    },
    [items, getCurrentScore, maxFinalMark, isGroupProject, projectId, fetchData]
  );

  const handleLockAll = useCallback(async () => {
    const unlocked = items.filter((item) => !item.isLocked && getCurrentScore(item) != null);

    if (!unlocked.length) {
      message.info('No unlocked items with final marks to lock');
      return;
    }

    const saved = await handleSaveAll(unlocked, { showSuccess: false, refreshAfterSave: false });
    if (!saved) return;

    setLoading(true);
    try {
      let successCount = 0;
      for (const record of unlocked) {
        const groupId = isGroupProject ? Number(record.groupId ?? record.id) : null;
        await lockFinalMark({
          projectId: Number(projectId),
          studentId: record.studentId ?? record.id,
          groupId,
          isLocked: 1,
        });
        successCount++;
      }
      setEditedScores({});
      message.success(`Confirmed ${successCount} final marks`);
      await fetchData();
    } catch (e) {
      console.error(e);
      message.error('Failed to confirm all marks, some may have been confirmed');
      await fetchData();
    } finally {
      setLoading(false);
    }
  }, [items, getCurrentScore, handleSaveAll, isGroupProject, projectId, fetchData]);

  const handlePublish = useCallback(async () => {
    if (hasUnsavedEdits) {
      message.warning('Please save all edited final marks before publishing');
      return;
    }
    const notConfirmed = items.filter((item) => !item.isLocked);
    if (notConfirmed.length > 0) {
      message.warning('All final marks must be admin confirmed before publishing');
      return;
    }
    const missingFinalScores = items.filter((item) => item.finalScore == null);
    if (missingFinalScores.length > 0) {
      message.warning('All rows must have final marks before publishing');
      return;
    }

    setPublishing(true);
    try {
      const res = await publishReport(projectId);
      const msgText = res?.msg || res?.message || res?.data?.msg || '';
      message.success(msgText);
    } catch (e) {
      console.error(e);
      message.error(
        e?.response?.data?.msg ||
        e?.response?.data?.message ||
        e?.message ||
        'Failed to send reports'
      );
    } finally {
      setPublishing(false);
    }
  }, [projectId, hasUnsavedEdits, items]);

  const columns = useMemo(() => {
    const cols = [];

    if (isGroupProject) {
      cols.push({
        title: 'Group ID',
        dataIndex: 'groupId',
        key: 'groupId',
        width: 100,
        render: (v, r) => v ?? r.id ?? '-',
      });
      cols.push({
        title: 'Group Name',
        key: 'name',
        width: 150,
        render: (_, r) => r.groupName ?? r.name ?? '-',
      });
      cols.push({
        title: 'Student ID',
        dataIndex: 'studentId',
        key: 'studentId',
        width: 120,
      });
      cols.push({
        title: 'Student Name',
        key: 'studentName',
        width: 180,
        render: (_, r) => getStudentName(r) || '-',
      });
    } else {
      cols.push({
        title: 'Student ID',
        dataIndex: 'studentId',
        key: 'studentId',
        width: 120,
      });
      cols.push({
        title: 'Name',
        key: 'name',
        width: 150,
        render: (_, r) => getStudentName(r) || r.name || '-',
      });
      cols.push({
        title: 'Email',
        dataIndex: 'email',
        key: 'email',
        ellipsis: true,
        width: 200,
      });
    }

    allMarkerNames.forEach(({ id, name }) => {
      cols.push({
        title: name,
        key: `marker-${id}`,
        width: 110,
        align: 'center',
        render: (_, record) => {
          const ms = (record.markerScores || []).find(
            (s) => s.markerId === id
          );
          if (!ms) {
            return <Tag color="default">Unassigned</Tag>;
          }
          if (ms.score == null) {
            return <Tag color="gold">Not Marked</Tag>;
          }
          return formatScore(ms?.score);
        },
      });
    });

    cols.push({
      title: 'Average',
      key: 'average',
      width: 90,
      align: 'center',
      render: (_, record) => {
        const scores = (record.markerScores || [])
          .map((s) => toScore(s.score))
          .filter((v) => v != null);
        if (!scores.length) return '--';
        const avg = scores.reduce((a, b) => a + b, 0) / scores.length;
        return <span className={styles.averageScore}>{avg.toFixed(2)}</span>;
      },
    });

    cols.push({
      title: 'Final Mark',
      key: 'finalMark',
      width: 190,
      align: 'center',
      render: (_, record) => {
        const locked = !!record.isLocked;
        const currentVal = getCurrentScore(record);
        return (
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            <InputNumber
              min={0}
              max={maxFinalMark}
              step={0.25}
              precision={2}
              value={currentVal}
              disabled={locked}
              onChange={(val) => handleScoreChange(record, val)}
              style={{ width: 92 }}
              size="small"
            />
            <Text type="secondary">/ {maxFinalMark.toFixed(2)}</Text>
          </div>
        );
      },
    });

    cols.push({
      title: 'Status',
      key: 'status',
      width: 140,
      align: 'center',
      render: (_, record) => {
        const scoredCount = (record.markerScores || []).filter(
          (s) => s.score != null
        ).length;

        if (record.isLocked) {
          return (
            <Tag icon={<CheckCircleOutlined />} color="blue">
              Admin Confirmed
            </Tag>
          );
        }

        if (scoredCount > 0) {
          return (
            <Tag icon={<CheckCircleOutlined />} color="success">
              {scoredCount} Marker{scoredCount > 1 ? 's' : ''} Scored
            </Tag>
          );
        }

        return <Tag color="default">Not Marked</Tag>;
      },
    });

    cols.push({
      title: 'Actions',
      key: 'actions',
      width: 160,
      align: 'center',
      fixed: 'right',
      render: (_, record) => {
        const key = getEditKey(record);
        const locked = !!record.isLocked;
        const isSaving = !!saving[key];
        const isLocking = !!locking[key];

        return (
          <div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}>
            <Tooltip title={locked ? 'Unconfirm to edit' : 'Save final mark'}>
              <Button
                type="link"
                size="small"
                icon={<SaveOutlined />}
                loading={isSaving}
                disabled={locked}
                onClick={() => handleSave(record)}
                className={`${styles.actionButton} ${styles.saveBtn}`}
              >
                Save
              </Button>
            </Tooltip>
            {locked ? (
              <Popconfirm
                title="Unconfirm this final mark?"
                description="This will allow the final mark to be edited again."
                onConfirm={() => handleLock(record, false)}
                okText="Unconfirm"
                cancelText="Cancel"
              >
                <Button
                  type="link"
                  size="small"
                  icon={<UnlockOutlined />}
                  loading={isLocking}
                  className={`${styles.actionButton} ${styles.unlockBtn}`}
                >
                  Unconfirm
                </Button>
              </Popconfirm>
            ) : (
              <Tooltip title="Admin confirm final mark">
                <Button
                  type="link"
                  size="small"
                  icon={<CheckCircleOutlined />}
                  loading={isLocking}
                  disabled={record.finalScore == null && getCurrentScore(record) == null}
                  onClick={() => handleConfirmWithSave(record)}
                  className={`${styles.actionButton} ${styles.lockBtn}`}
                >
                  Confirm
                </Button>
              </Tooltip>
            )}
          </div>
        );
      },
    });

    return cols;
  }, [
    isGroupProject,
    allMarkerNames,
    getCurrentScore,
    handleScoreChange,
    handleSave,
    handleConfirmWithSave,
    handleLock,
    saving,
    locking,
    getEditKey,
    maxFinalMark,
  ]);

  const pageTitle = subjectName && projectName
    ? `${subjectName} - ${projectName} - Final Mark`
    : `${projectName} - Final Mark`;

  return (
    <div className={styles.detailsPage}>
      <div className={styles.header}>
        <BackButton />
        <Title level={2} className={styles.pageTitle}>
          {pageTitle}
        </Title>
        <div className={styles.headerActions}>
          <Button
            icon={<SaveOutlined />}
            className={styles.lockAllButton}
            loading={savingAll}
            onClick={() => handleSaveAll()}
          >
            Save All
          </Button>
          <Popconfirm
            title="Confirm all final marks?"
            description="This will save all entered final marks first, then admin confirm them."
            onConfirm={handleLockAll}
            okText="Confirm All"
            cancelText="Cancel"
          >
            <Button
              icon={<LockOutlined />}
              className={styles.lockAllButton}
              loading={loading}
            >
              Confirm All
            </Button>
          </Popconfirm>
          <Tooltip
            title={
              !allConfirmed
                ? 'All items must be admin confirmed before publishing'
                : hasUnsavedEdits
                  ? 'Please save all edited final marks before publishing'
                  : ''
            }
          >
            <Popconfirm
              title="Publish grade reports?"
              description="This will send assessment report emails to all students, markers, and admins."
              onConfirm={handlePublish}
              okText="Publish"
              cancelText="Cancel"
              disabled={!allConfirmed || hasUnsavedEdits}
            >
              <Button
                icon={<SendOutlined />}
                className={styles.publishButton}
                loading={publishing}
                disabled={!allConfirmed || hasUnsavedEdits}
              >
                Publish
              </Button>
            </Popconfirm>
          </Tooltip>
        </div>
      </div>

      <div className={styles.mainContent}>
        <Card
          className={styles.tableCard}
          title={
            <Text strong>
              {isGroupProject ? 'Group Final Marks' : 'Student Final Marks'}
            </Text>
          }
          variant={"outlined"}
        >
          <Table
            rowKey={getRowKey}
            size="middle"
            dataSource={items}
            columns={columns}
            pagination={pagination}
            loading={loading}
            scroll={{ x: 'max-content' }}
          />
        </Card>
      </div>
    </div>
  );
}

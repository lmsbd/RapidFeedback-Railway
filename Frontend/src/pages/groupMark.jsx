import { useEffect, useMemo, useState, useCallback } from 'react';
import { history, useLocation } from 'umi';
import {
  Button,
  Card,
  Input,
  InputNumber,
  Space,
  Spin,
  Table,
  Typography,
  message,
} from 'antd';
import { EditOutlined } from '@ant-design/icons';
import BackButton from '../components/BackButton/BackButton';
import {
  getProjectDetail,
  getStudentAssessmentScores,
} from '../apis/getProjectDetail';
import { getGroupMark, saveGroupMark } from '../apis/mark';
import styles from './groupMark.module.less';

const { Title, Text } = Typography;
const { TextArea } = Input;

function getStudentName(student) {
  return `${student?.firstName ?? ''} ${student?.surname ?? ''}`.trim();
}

function calcTotalScore(assessments) {
  if (!Array.isArray(assessments) || assessments.length === 0) return null;
  const hasWeighting = assessments.some((a) =>
    Number.isFinite(Number(a.weighting))
  );
  if (!hasWeighting) {
    return assessments.reduce((sum, a) => sum + (Number(a.score) || 0), 0);
  }
  return assessments.reduce((sum, a) => {
    const score = Number(a.score) || 0;
    const weighting = Number(a.weighting) || 0;
    return sum + (score * weighting) / 100;
  }, 0);
}

function getGroupCommentCacheKey(projectId, groupId) {
  if (!projectId || !groupId) return '';
  return `groupMarkCommentDraft_${projectId}_${groupId}`;
}

export default function GroupMarkPage() {
  const location = useLocation();
  const searchParams = useMemo(
    () => new URLSearchParams(location.search),
    [location.search]
  );

  const projectId = searchParams.get('projectId') || '';
  const groupId = searchParams.get('groupId') || '';
  const rawGroupName = searchParams.get('groupName') || '';
  const groupName = rawGroupName ? decodeURIComponent(rawGroupName) : '';
  const pageType = searchParams.get('type') || 'mark';
  const isReview = pageType === 'review';

  const [loading, setLoading] = useState(false);
  const [students, setStudents] = useState([]);
  const [studentScores, setStudentScores] = useState({});
  // per-student group score: { [studentId]: number }
  const [groupScores, setGroupScores] = useState({});
  const [groupComment, setGroupComment] = useState('');
  const [saving, setSaving] = useState(false);
  const [projectMaxScore, setProjectMaxScore] = useState(null);
  const commentCacheKey = useMemo(
    () => getGroupCommentCacheKey(projectId, groupId),
    [projectId, groupId]
  );

  const readCachedComment = useCallback(() => {
    if (!commentCacheKey) return null;
    const cached = sessionStorage.getItem(commentCacheKey);
    return cached === null ? null : cached;
  }, [commentCacheKey]);

  const writeCachedComment = useCallback(
    (value) => {
      if (!commentCacheKey) return;
      sessionStorage.setItem(commentCacheKey, String(value ?? ''));
    },
    [commentCacheKey]
  );

  const clearCachedComment = useCallback(() => {
    if (!commentCacheKey) return;
    sessionStorage.removeItem(commentCacheKey);
  }, [commentCacheKey]);

  const fetchData = useCallback(async () => {
    if (!projectId || !groupId) return;
    setLoading(true);
    try {
      const res = await getProjectDetail(projectId);
      const weightedMaxScore = Number(res?.weightedMaxScore);
      const normalizedMaxScore =
        Number.isFinite(weightedMaxScore) && weightedMaxScore >= 0
          ? weightedMaxScore
          : null;
      setProjectMaxScore(normalizedMaxScore);

      const teams = Array.isArray(res?.teams) ? res.teams : [];
      const team = teams.find((t) => String(t?.id) === String(groupId));
      const members = Array.isArray(team?.students) ? team.students : [];
      setStudents(members);

      const scoreMap = {};
      await Promise.all(
        members.map(async (student) => {
          const sid = student?.id ?? student?.studentId;
          if (!sid) return;
          try {
            const scoreRes = await getStudentAssessmentScores(projectId, sid);
            if (scoreRes) {
              const descArr = Array.isArray(scoreRes?.description)
                ? scoreRes.description
                : [];
              const first = descArr[0] || null;
              const assessments = Array.isArray(first?.assessment)
                ? first.assessment
                : [];
              const hasScores = assessments.some(
                (a) => a?.score !== null && a?.score !== undefined
              );
              if (hasScores) {
                const total = calcTotalScore(assessments);
                scoreMap[String(sid)] = { total, assessments };
              }
            }
          } catch {
            // student not yet marked
          }
        })
      );
      setStudentScores(scoreMap);

      // Try fetching saved group mark data from backend
      if (isReview) {
        // Review mode: fetch saved group mark from backend only
        try {
          const gmRes = await getGroupMark(projectId, groupId);
          const savedGroupMark = gmRes?.data;
          const initialGroupScores = {};
          members.forEach((s) => {
            const sid = String(s?.id ?? s?.studentId);
            const saved = savedGroupMark?.students?.find(
              (st) => String(st.studentId) === sid
            );
            const savedScore = Number(saved?.groupScore ?? 0);
            initialGroupScores[sid] =
              normalizedMaxScore !== null
                ? Math.min(savedScore, normalizedMaxScore)
                : savedScore;
          });
          setGroupScores(initialGroupScores);
          const cachedComment = readCachedComment();
          setGroupComment(
            cachedComment !== null
              ? cachedComment
              : savedGroupMark?.comment || ''
          );
        } catch {
          const initialGroupScores = {};
          members.forEach((s) => {
            const sid = String(s?.id ?? s?.studentId);
            initialGroupScores[sid] = 0;
          });
          setGroupScores(initialGroupScores);
          const cachedComment = readCachedComment();
          setGroupComment(cachedComment !== null ? cachedComment : '');
        }
      } else {
        // Mark mode: use average of individual scores as default
        const totals = Object.values(scoreMap)
          .map((s) => s.total)
          .filter((t) => t !== null && Number.isFinite(t));
        const avg =
          totals.length > 0
            ? Number(
                (totals.reduce((a, b) => a + b, 0) / totals.length).toFixed(2)
              )
            : 0;
        const initialGroupScores = {};
        members.forEach((s) => {
          const sid = String(s?.id ?? s?.studentId);
          initialGroupScores[sid] =
            normalizedMaxScore !== null
              ? Math.min(avg, normalizedMaxScore)
              : avg;
        });
        setGroupScores(initialGroupScores);
        const cachedComment = readCachedComment();
        setGroupComment(cachedComment !== null ? cachedComment : '');
      }
    } catch (e) {
      console.error(e);
      message.error('Failed to load group data');
    } finally {
      setLoading(false);
    }
  }, [projectId, groupId, isReview, readCachedComment]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleMarkStudent = (student) => {
    const sid = student?.id ?? student?.studentId;
    const name = getStudentName(student);
    const params = new URLSearchParams({
      projectId: String(projectId),
      individualId: String(sid),
      studentId: String(student?.studentId ?? ''),
      studentName: name || 'Student',
      type: 'mark',
      fromGroup: '1',
      returnGroupId: String(groupId),
      returnGroupName: groupName,
    });
    history.push(`/mark?${params.toString()}`);
  };

  const handleReviewStudent = (student) => {
    const sid = student?.id ?? student?.studentId;
    const name = getStudentName(student);
    const params = new URLSearchParams({
      projectId: String(projectId),
      individualId: String(sid),
      studentId: String(student?.studentId ?? ''),
      studentName: name || 'Student',
      type: 'review',
      fromGroup: '1',
      returnGroupId: String(groupId),
      returnGroupName: groupName,
    });
    history.push(`/mark?${params.toString()}`);
  };

  const handleGroupScoreChange = (sid, value) => {
    const numericValue = Number(value ?? 0);
    const nextValue =
      projectMaxScore !== null
        ? Math.min(Math.max(numericValue, 0), projectMaxScore)
        : Math.max(numericValue, 0);
    setGroupScores((prev) => ({ ...prev, [sid]: nextValue }));
  };

  const handleSave = async () => {
    if (!projectId || !groupId) {
      message.error('Missing project or group info');
      return;
    }

    const allScored = students.every((s) => {
      const sid = String(s?.id ?? s?.studentId);
      return !!studentScores[sid];
    });

    if (!allScored) {
      message.error('Please mark all students before saving the group score');
      return;
    }

    const studentsPayload = students.map((s) => {
      const sid = String(s?.id ?? s?.studentId);
      const rawScore = Number(groupScores[sid] ?? 0);
      const validScore = Number.isFinite(rawScore) ? rawScore : 0;
      const cappedScore =
        projectMaxScore !== null
          ? Math.min(validScore, projectMaxScore)
          : validScore;
      return {
        studentId: Number(sid),
        groupScore: cappedScore,
      };
    });

    setSaving(true);
    try {
      const res = await saveGroupMark({
        projectId: Number(projectId),
        groupId: Number(groupId),
        comment: groupComment,
        students: studentsPayload,
      });
      if (res?.code === 200) {
        message.success('Group score saved');
        clearCachedComment();
        history.push(`/markedList/${projectId}`);
      } else {
        message.error(res?.message || 'Failed to save');
      }
    } catch (e) {
      const backendMessage = e?.response?.data?.message;
      message.error(backendMessage || e?.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  const handleBack = () => {
    clearCachedComment();
    history.push(`/markedList/${projectId}`);
  };

  const columns = useMemo(
    () => [
      {
        title: 'ID',
        key: 'id',
        width: 120,
        render: (_, r) => r?.studentId ?? r?.id ?? '-',
      },
      {
        title: 'Name',
        key: 'name',
        render: (_, r) => getStudentName(r),
      },
      {
        title: 'Individual Score',
        key: 'individualScore',
        width: 180,
        render: (_, r) => {
          const sid = String(r?.id ?? r?.studentId);
          const scoreData = studentScores[sid];
          if (!scoreData) {
            return (
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleMarkStudent(r)}
              >
                Mark
              </Button>
            );
          }
          return (
            <Space>
              <Text strong>{Number(scoreData.total).toFixed(2)}</Text>
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleReviewStudent(r)}
              >
                Edit
              </Button>
            </Space>
          );
        },
      },
      {
        title: 'Group Score',
        key: 'groupScore',
        width: 220,
        render: (_, r) => {
          const sid = String(r?.id ?? r?.studentId);
          return (
            <Space size={6}>
              <InputNumber
                value={groupScores[sid] ?? 0}
                min={0}
                max={projectMaxScore ?? undefined}
                step={0.01}
                precision={2}
                onChange={(v) => handleGroupScoreChange(sid, v)}
                style={{ width: 110 }}
                disabled={true}
              />
              {projectMaxScore !== null && (
                <Text type="secondary">/ {projectMaxScore.toFixed(2)}</Text>
              )}
            </Space>
          );
        },
      },
    ],
    [
      studentScores,
      groupScores,
      projectMaxScore,
      isReview,
      projectId,
      groupId,
      groupName,
    ]
  );

  const markedCount = students.filter((s) => {
    const sid = String(s?.id ?? s?.studentId);
    return !!studentScores[sid];
  }).length;

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <BackButton onClick={handleBack} text="Project" />
        <div className={styles.headerMain}>
          <Title level={3} className={styles.title}>
            {groupName || 'Group'}
          </Title>
          <div className={styles.metaRow}>
            <Text type="secondary">
              {markedCount} / {students.length} students marked
            </Text>
          </div>
        </div>
      </div>

      <div className={styles.content}>
        <Card
          className={styles.card}
          title={<Text strong>Group Students</Text>}
        >
          <Spin spinning={loading}>
            <Table
              rowKey={(r) => String(r?.id ?? r?.studentId)}
              dataSource={students}
              columns={columns}
              pagination={false}
              size="middle"
            />
          </Spin>
        </Card>

        <Card className={styles.scoreCard}>
          <Text strong className={styles.sectionLabel}>
            Group Comment
          </Text>
          <TextArea
            value={groupComment}
            placeholder="Enter group comment..."
            autoSize={{ minRows: 3, maxRows: 6 }}
            onChange={(e) => {
              const nextComment = e.target.value;
              setGroupComment(nextComment);
              writeCachedComment(nextComment);
            }}
            style={{ marginTop: 8 }}
          />
        </Card>
      </div>

      <div className={styles.footer}>
        <div />
        <Button
          className={styles.saveButton}
          type="primary"
          loading={saving}
          onClick={handleSave}
        >
          Save
        </Button>
      </div>
    </div>
  );
}

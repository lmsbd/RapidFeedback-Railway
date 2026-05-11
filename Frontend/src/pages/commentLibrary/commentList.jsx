import React, { useEffect, useMemo, useState } from 'react';
import { useParams, useLocation } from 'umi';
import BackButton from '../../components/BackButton/BackButton';
import {
  Card,
  Button,
  Typography,
  List,
  Tag,
  message,
  Modal,
  Form,
  Input,
  Radio,
  Spin,
  Upload,
  Space,
  Alert,
} from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
  UploadOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import { getCommentList, saveComment, deleteComment } from '@/apis/comment';
import Papa from 'papaparse';
import styles from './commentList.module.less';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { confirm } = Modal;

const CommentList = () => {
  const { categoryId } = useParams();
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  const [comments, setComments] = useState({
    positive: [],
    neutral: [],
    negative: [],
  });
  const [categoryName, setCategoryName] = useState('');
  const [elementName, setElementName] = useState('');
  const [modalVisible, setModalVisible] = useState(false);
  const [editingComment, setEditingComment] = useState(null);
  const [form] = Form.useForm();
  const [importModalVisible, setImportModalVisible] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importFileName, setImportFileName] = useState('');
  const [importRows, setImportRows] = useState([]);
  const [importIssues, setImportIssues] = useState([]);

  useEffect(() => {
    // Get element name from URL query parameters
    const urlParams = new URLSearchParams(location.search);
    const elementNameFromUrl = urlParams.get('elementName');
    if (elementNameFromUrl) {
      setElementName(decodeURIComponent(elementNameFromUrl));
    }

    fetchComments();
  }, [categoryId, location.search]);

  const fetchComments = async () => {
    try {
      setLoading(true);
      const response = await getCommentList(Number(categoryId));

      if (response.code === 200 && response.data) {
        // Group comments by type
        const grouped = {
          positive: [],
          neutral: [],
          negative: [],
        };

        response.data.forEach((comment) => {
          // commentType: 2=positive, 1=neutral, 0=negative
          if (comment.commentType === 2) {
            grouped.positive.push(comment);
          } else if (comment.commentType === 1) {
            grouped.neutral.push(comment);
          } else if (comment.commentType === 0) {
            grouped.negative.push(comment);
          }
        });

        setComments(grouped);

        // Get category name from first comment if available
        if (response.data.length > 0 && response.data[0].templateElementName) {
          setCategoryName(response.data[0].templateElementName);
        }
      } else {
        message.error('Failed to load comments');
      }
    } catch (error) {
      console.error('Error fetching comments:', error);
      message.error('Failed to load comments');
    } finally {
      setLoading(false);
    }
  };

  const handleAddComment = () => {
    setEditingComment(null);
    form.resetFields();
    form.setFieldsValue({ commentType: 2 }); // Default to positive
    setModalVisible(true);
  };

  const handleEditComment = (comment) => {
    setEditingComment(comment);
    form.setFieldsValue({
      content: comment.content,
      commentType: comment.commentType,
    });
    setModalVisible(true);
  };

  const handleDeleteComment = (commentId) => {
    confirm({
      title: 'Are you sure you want to delete this comment?',
      content: 'This action cannot be undone.',
      okText: 'Delete',
      okType: 'danger',
      cancelText: 'Cancel',
      onOk: async () => {
        try {
          const response = await deleteComment(commentId);
          if (response.code === 200) {
            message.success('Comment deleted successfully');
            fetchComments(); // Refresh list
          } else {
            message.error(response.msg || 'Failed to delete comment');
          }
        } catch (error) {
          console.error('Error deleting comment:', error);
          message.error('Failed to delete comment');
        }
      },
    });
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();

      const commentData = {
        id: editingComment ? editingComment.id : null,
        templateElementId: Number(categoryId),
        content: values.content,
        commentType: values.commentType,
      };

      const response = await saveComment(commentData);

      if (response.code === 200) {
        message.success(
          editingComment
            ? 'Comment updated successfully'
            : 'Comment added successfully'
        );
        setModalVisible(false);
        form.resetFields();
        fetchComments(); // Refresh list
      } else {
        message.error(response.msg || 'Failed to save comment');
      }
    } catch (error) {
      console.error('Error saving comment:', error);
      message.error('Failed to save comment');
    }
  };

  const handleModalCancel = () => {
    setModalVisible(false);
    form.resetFields();
  };

  const normalizeCommentType = (value) => {
    if (value === undefined || value === null) return null;
    const raw = String(value).trim();
    if (!raw) return null;

    if (raw === '2') return 2;
    if (raw === '1') return 1;
    if (raw === '0') return 0;

    const s = raw.toLowerCase();
    if (['positive', 'pos', 'p', 'good', '好评', '正面'].includes(s)) return 2;
    if (['neutral', 'neu', 'n', '中评', '中性'].includes(s)) return 1;
    if (['negative', 'neg', 'bad', '差评', '负面'].includes(s)) return 0;

    return null;
  };

  const downloadImportTemplate = () => {
    const rows = [
      { content: 'Excellent structure and flow', commentType: 'positive' },
      { content: 'Adequate structure', commentType: 'neutral' },
      { content: 'Lacks clear structure', commentType: 'negative' },
    ];
    const csv = `\ufeff${Papa.unparse(rows, { columns: ['content', 'commentType'] })}`;
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'comment_import_template.csv';
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  };

  const resetImportState = () => {
    setImportFileName('');
    setImportRows([]);
    setImportIssues([]);
    setImporting(false);
  };

  const handleOpenImportModal = () => {
    resetImportState();
    setImportModalVisible(true);
  };

  const handleCloseImportModal = () => {
    setImportModalVisible(false);
    resetImportState();
  };

  const parseImportCsv = async (file) => {
    const text = await file.text();
    const parsed = Papa.parse(text, {
      header: true,
      skipEmptyLines: true,
    });

    const fields = (parsed.meta?.fields || []).map((f) =>
      String(f || '')
        .trim()
        .toLowerCase()
    );
    const hasContent = fields.includes('content');
    const hasCommentType = fields.includes('commenttype');

    const issues = [];
    if (!hasContent) {
      issues.push('Missing required column: content');
    }
    if (!hasCommentType) {
      issues.push('Missing required column: commentType');
    }

    const rows = [];
    (parsed.data || []).forEach((row, index) => {
      const normalized = {};
      Object.keys(row || {}).forEach((k) => {
        normalized[
          String(k || '')
            .trim()
            .toLowerCase()
        ] = row[k];
      });

      const content = String(normalized.content ?? '').trim();
      const commentType = normalizeCommentType(normalized.commenttype);

      if (!content && !normalized.commenttype) return;

      const rowIssues = [];
      if (!content) rowIssues.push('content is empty');
      if (content && (content.length < 2 || content.length > 500))
        rowIssues.push('content length must be 2-500');
      if (commentType === null) rowIssues.push('invalid commentType');

      rows.push({
        _rowNumber: index + 2,
        content,
        commentType,
        _issues: rowIssues,
      });
    });

    if (parsed.errors?.length) {
      parsed.errors.slice(0, 5).forEach((e) => {
        issues.push(`Parse error: ${e.message}`);
      });
      if (parsed.errors.length > 5) {
        issues.push(`Parse error: +${parsed.errors.length - 5} more`);
      }
    }

    return { rows, issues };
  };

  const handleImportBeforeUpload = async (file) => {
    const isCsv =
      file.name.toLowerCase().endsWith('.csv') || file.type === 'text/csv';
    if (!isCsv) {
      message.error('Please select a .csv file');
      return Upload.LIST_IGNORE;
    }

    try {
      setImportFileName(file.name);
      const { rows, issues } = await parseImportCsv(file);
      setImportRows(rows);
      setImportIssues(issues);
      if (!rows.length) {
        message.warning('No rows found in the file');
      }
    } catch (e) {
      console.error(e);
      message.error('Failed to parse file');
      setImportRows([]);
      setImportIssues(['Failed to parse file']);
    }

    return false;
  };

  const importSummary = useMemo(() => {
    const total = importRows.length;
    const invalid = importRows.filter(
      (r) => (r._issues || []).length > 0
    ).length;
    const positive = importRows.filter(
      (r) => r.commentType === 2 && (r._issues || []).length === 0
    ).length;
    const neutral = importRows.filter(
      (r) => r.commentType === 1 && (r._issues || []).length === 0
    ).length;
    const negative = importRows.filter(
      (r) => r.commentType === 0 && (r._issues || []).length === 0
    ).length;
    const valid = total - invalid;
    return { total, valid, invalid, positive, neutral, negative };
  }, [importRows]);

  const handleImportOk = async () => {
    const hardBlocked = importIssues.some((i) =>
      i.startsWith('Missing required column')
    );
    if (hardBlocked) {
      message.error('Please fix the template columns and re-upload');
      return;
    }

    const validRows = importRows.filter((r) => (r._issues || []).length === 0);
    if (!validRows.length) {
      message.error('No valid rows to import');
      return;
    }

    setImporting(true);
    try {
      let successCount = 0;
      let failCount = 0;

      const batchSize = 5;
      for (let i = 0; i < validRows.length; i += batchSize) {
        const batch = validRows.slice(i, i + batchSize);
        const results = await Promise.allSettled(
          batch.map((r) =>
            saveComment({
              id: null,
              templateElementId: Number(categoryId),
              content: r.content,
              commentType: r.commentType,
            })
          )
        );

        results.forEach((res) => {
          if (res.status === 'fulfilled' && res.value?.code === 200) {
            successCount += 1;
          } else {
            failCount += 1;
          }
        });
      }

      if (failCount === 0) {
        message.success(`Imported ${successCount} comments`);
      } else if (successCount === 0) {
        message.error(`Import failed (${failCount} rows)`);
      } else {
        message.warning(
          `Imported ${successCount} comments, ${failCount} failed`
        );
      }

      await fetchComments();
      setImportModalVisible(false);
      resetImportState();
    } catch (e) {
      console.error(e);
      message.error('Import failed');
    } finally {
      setImporting(false);
    }
  };

  const getTagColor = (type) => {
    switch (type) {
      case 2:
        return 'green';
      case 0:
        return 'red';
      case 1:
        return 'blue';
      default:
        return 'default';
    }
  };

  const getTypeLabel = (type) => {
    switch (type) {
      case 2:
        return 'Positive';
      case 0:
        return 'Negative';
      case 1:
        return 'Neutral';
      default:
        return 'Unknown';
    }
  };

  const renderCommentSection = (title, commentList, typeValue, color) => (
    <Card
      title={
        <div className={styles.sectionTitle}>
          <Tag color={color} className={styles.typeTag}>
            {title}
          </Tag>
          <Text className={styles.count}>({commentList.length})</Text>
        </div>
      }
      className={styles.commentSection}
    >
      {commentList.length === 0 ? (
        <div className={styles.emptyState}>
          <Text type="secondary">No {title.toLowerCase()} comments yet</Text>
        </div>
      ) : (
        <List
          dataSource={commentList}
          renderItem={(comment) => (
            <List.Item
              className={styles.commentItem}
              actions={[
                <Button
                  key="edit"
                  type="text"
                  icon={<EditOutlined />}
                  onClick={() => handleEditComment(comment)}
                  className={styles.actionButton}
                >
                  Edit
                </Button>,
                <Button
                  key="delete"
                  type="text"
                  danger
                  icon={<DeleteOutlined />}
                  onClick={() => handleDeleteComment(comment.id)}
                  className={styles.actionButton}
                >
                  Delete
                </Button>,
              ]}
            >
              <div className={styles.commentContent}>
                <Text>{comment.content}</Text>
              </div>
            </List.Item>
          )}
        />
      )}
    </Card>
  );

  if (loading) {
    return (
      <div className={styles.commentListPage}>
        <div className={styles.header}>
          <BackButton />
          <Title level={2} className={styles.pageTitle}>
            {elementName || 'Category'} - Comment List
          </Title>
        </div>
        <div className={styles.loadingContainer}>
          <Spin size="large" tip="Loading comments..." />
        </div>
      </div>
    );
  }

  return (
    <div className={styles.commentListPage}>
      <div className={styles.header}>
        <BackButton />
        <Title level={2} className={styles.pageTitle}>
          {elementName}
        </Title>
        <div className={styles.headerActions}>
          <Button icon={<DownloadOutlined />} onClick={downloadImportTemplate}>
            Download Template
          </Button>
          <Button
            type="primary"
            icon={<UploadOutlined />}
            onClick={handleOpenImportModal}
          >
            Import
          </Button>
        </div>
      </div>
      <div className={styles.mainContent}>
        {renderCommentSection('Positive', comments.positive, 2, 'green')}
        {renderCommentSection('Neutral', comments.neutral, 1, 'blue')}
        {renderCommentSection('Negative', comments.negative, 0, 'red')}
      </div>
      <div className={styles.addButtonContainer}>
        <Button
          type="primary"
          size="large"
          icon={<PlusOutlined />}
          onClick={handleAddComment}
          className={styles.addButton}
        >
          Add Comment
        </Button>
      </div>

      <Modal
        title="Import Comments"
        open={importModalVisible}
        onOk={handleImportOk}
        onCancel={handleCloseImportModal}
        okText={importing ? 'Importing...' : 'Import'}
        cancelText="Cancel"
        okButtonProps={{
          disabled:
            importing ||
            !importRows.length ||
            importIssues.some((i) => i.startsWith('Missing required column')),
        }}
        confirmLoading={importing}
        width={720}
      >
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <Alert
            type="info"
            showIcon
            message="CSV format"
            description={
              <div>
                <div>Required columns: content, commentType</div>
                <div>
                  commentType accepted values: 2/1/0 or
                  positive/neutral/negative
                </div>
                <div>
                  All imported comments will be added to the current page
                  category.
                </div>
              </div>
            }
          />

          <Upload
            accept=".csv,text/csv"
            maxCount={1}
            beforeUpload={handleImportBeforeUpload}
            showUploadList={false}
          >
            <Button icon={<UploadOutlined />}>Select CSV File</Button>
          </Upload>

          {importFileName ? (
            <Text type="secondary">Selected: {importFileName}</Text>
          ) : null}

          {importIssues.length ? (
            <Alert
              type="warning"
              showIcon
              message="Issues"
              description={
                <div>
                  {importIssues.map((i) => (
                    <div key={i}>{i}</div>
                  ))}
                </div>
              }
            />
          ) : null}

          {importRows.length ? (
            <Alert
              type={importSummary.invalid ? 'warning' : 'success'}
              showIcon
              message="Preview"
              description={
                <div>
                  <div>
                    Total: {importSummary.total}, Valid: {importSummary.valid},
                    Invalid: {importSummary.invalid}
                  </div>
                  <div>
                    Valid breakdown: Positive {importSummary.positive}, Neutral{' '}
                    {importSummary.neutral}, Negative {importSummary.negative}
                  </div>
                </div>
              }
            />
          ) : null}

          {importRows.length ? (
            <List
              bordered
              size="small"
              style={{ maxHeight: 260, overflow: 'auto' }}
              dataSource={importRows.slice(0, 20)}
              renderItem={(r) => (
                <List.Item>
                  <Space
                    style={{ width: '100%', justifyContent: 'space-between' }}
                  >
                    <Space>
                      <Tag color={getTagColor(r.commentType)}>
                        {getTypeLabel(r.commentType)}
                      </Tag>
                      {r.content ? (
                        <Text>{r.content}</Text>
                      ) : (
                        <Text type="secondary">(empty)</Text>
                      )}
                    </Space>
                    <Space>
                      <Text type="secondary">Row {r._rowNumber}</Text>
                      {(r._issues || []).length ? (
                        <Tag color="red">Invalid</Tag>
                      ) : (
                        <Tag color="green">OK</Tag>
                      )}
                    </Space>
                  </Space>
                </List.Item>
              )}
            />
          ) : null}

          {importRows.length > 20 ? (
            <Text type="secondary">Showing first 20 rows</Text>
          ) : null}
        </Space>
      </Modal>

      {/* Add/Edit Comment Modal */}
      <Modal
        title={editingComment ? 'Edit Comment' : 'Add Comment'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={handleModalCancel}
        width={600}
        okText="Save"
        cancelText="Cancel"
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            commentType: 2,
          }}
        >
          <Form.Item
            name="commentType"
            label="Comment Type"
            rules={[
              { required: true, message: 'Please select a comment type' },
            ]}
          >
            <Radio.Group>
              <Radio.Button value={2} style={{ color: '#52c41a' }}>
                Positive
              </Radio.Button>
              <Radio.Button value={1} style={{ color: '#1890ff' }}>
                Neutral
              </Radio.Button>
              <Radio.Button value={0} style={{ color: '#ff4d4f' }}>
                Negative
              </Radio.Button>
            </Radio.Group>
          </Form.Item>
          <Form.Item
            name="content"
            label="Comment Content"
            rules={[
              { required: true, message: 'Please enter comment content' },
              { min: 2, message: 'Content must be at least 2 characters' },
              { max: 500, message: 'Content cannot exceed 500 characters' },
            ]}
          >
            <TextArea
              rows={4}
              placeholder="Enter comment content"
              showCount
              maxLength={500}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CommentList;

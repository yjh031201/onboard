const { Schema, model } = require('mongoose');

const cardSchema = new Schema(
  {
    boardId: { type: Schema.Types.ObjectId, ref: 'Board', required: true, index: true },
    columnId: { type: Schema.Types.ObjectId, ref: 'Column', required: true, index: true },
    title: { type: String, required: true, trim: true },
    description: { type: String, default: '' },
    position: { type: Number, required: true },
    assigneeId: { type: Schema.Types.ObjectId, ref: 'User', default: null },
    labels: { type: [Schema.Types.ObjectId], default: [] },
    dueDate: { type: Date, default: null },
    createdBy: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  },
  { timestamps: true }
);

cardSchema.index({ columnId: 1, position: 1 });

module.exports = model('Card', cardSchema);

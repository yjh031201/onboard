const { Schema, model } = require('mongoose');

const columnSchema = new Schema(
  {
    boardId: { type: Schema.Types.ObjectId, ref: 'Board', required: true, index: true },
    title: { type: String, required: true, trim: true },
    position: { type: Number, required: true },
  },
  { timestamps: true }
);

columnSchema.index({ boardId: 1, position: 1 });

module.exports = model('Column', columnSchema);

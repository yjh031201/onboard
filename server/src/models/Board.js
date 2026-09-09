const { Schema, model } = require('mongoose');

const memberSchema = new Schema(
  {
    userId: { type: Schema.Types.ObjectId, ref: 'User', required: true },
    role: { type: String, enum: ['owner', 'member'], required: true },
  },
  { _id: false }
);

const labelSchema = new Schema(
  {
    name: { type: String, required: true },
    color: { type: String, required: true },
  }
);

const boardSchema = new Schema(
  {
    title: { type: String, required: true, trim: true },
    ownerId: { type: Schema.Types.ObjectId, ref: 'User', required: true },
    members: { type: [memberSchema], default: [] },
    labels: { type: [labelSchema], default: [] },
  },
  { timestamps: true }
);

boardSchema.index({ 'members.userId': 1 });

module.exports = model('Board', boardSchema);
